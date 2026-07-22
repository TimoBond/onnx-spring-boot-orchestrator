import torch
import torch.nn as nn
import numpy as np


FEATURE_MIN = np.array([0.0,  0.0,  0.0,   0.0])
FEATURE_MAX = np.array([600.0, 100.0, 1.0, 100.0])

def normalize(x):
    return (x - FEATURE_MIN) / (FEATURE_MAX - FEATURE_MIN)

def generate_normal_sequence(n=300):
    data = []
    for _ in range(n):
        seq = []
        for _ in range(10):
            raw = np.array([
                np.random.normal(50, 4),
                np.random.normal(5, 1.5),
                np.random.normal(0.01, 0.003),
                 np.random.normal(38, 8),        
            ])
            seq.append(normalize(raw))
        data.append(seq)
    return np.array(data, dtype=np.float32)

X_train = generate_normal_sequence(300)
print(f"Train shape: {X_train.shape}")
print(f"Value range: [{X_train.min():.3f}, {X_train.max():.3f}]")

class LSTMAutoencoder(nn.Module):
    def __init__(self, input_size=4, hidden_size=16, num_layers=1):
        super().__init__()
        self.encoder = nn.LSTM(input_size, hidden_size,
                               num_layers, batch_first=True)
        self.decoder = nn.LSTM(hidden_size, input_size,
                               num_layers, batch_first=True)

    def forward(self, x):
        _, (hidden, _) = self.encoder(x)
        repeated = hidden[-1].unsqueeze(1).repeat(1, x.size(1), 1)
        output, _ = self.decoder(repeated)
        return output

model = LSTMAutoencoder()
optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
criterion = nn.MSELoss()
X_tensor = torch.tensor(X_train)

print("Training...")
for epoch in range(200):
    model.train()
    optimizer.zero_grad()
    output = model(X_tensor)
    loss = criterion(output, X_tensor)
    loss.backward()
    optimizer.step()
    if (epoch + 1) % 40 == 0:
        print(f"Epoch {epoch+1}/200 | Loss: {loss.item():.6f}")

print("Training completed")

model.eval()
with torch.no_grad():
    reconstructed = model(X_tensor)
    errors = ((reconstructed - X_tensor) ** 2).mean(dim=(1, 2))
    threshold = float(errors.mean() + 3 * errors.std())
    print(f"MSE: mean={errors.mean():.6f}, std={errors.std():.6f}")
    print(f"Threshold: {threshold:.6f}")

def make_seq(latency, cpu, error, threads):
    raw = np.array([[latency, cpu, error, threads]] * 10, dtype=np.float32)
    return torch.tensor(normalize(raw).astype(np.float32)).unsqueeze(0)

with torch.no_grad():
    e_norm = ((model(make_seq(50, 5, 0.01, 32)) -
               make_seq(50, 5, 0.01, 32)) ** 2).mean().item()
    e_anom = ((model(make_seq(500, 95, 0.9, 100)) -
               make_seq(500, 95, 0.9, 100)) ** 2).mean().item()

print(f"\nNorm  MSE: {e_norm:.6f} → {'ANOMALY' if e_norm > threshold else 'НОРМА'}")
print(f"ANOMALY MSE: {e_anom:.6f} → {'ANOMALY' if e_anom > threshold else 'НОРМА'}")

torch.save({'model_state': model.state_dict(), 'threshold': threshold},
           'lstm_autoencoder.pt')
print("\n saved → lstm_autoencoder.pt")

np.save('feature_min.npy', FEATURE_MIN)
np.save('feature_max.npy', FEATURE_MAX)
print(" normalization saved")
