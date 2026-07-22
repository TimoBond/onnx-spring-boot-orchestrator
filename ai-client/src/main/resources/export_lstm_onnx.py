import torch
import torch.nn as nn
import numpy as np

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


checkpoint = torch.load('lstm_autoencoder.pt')
model = LSTMAutoencoder()
model.load_state_dict(checkpoint['model_state'])
model.eval()
threshold = checkpoint['threshold']
print(f"Threshold: {threshold:.6f}")


dummy_input = torch.randn(1, 10, 4)


torch.onnx.export(
    model,
    dummy_input,
    "lstm_autoencoder.onnx",
    input_names=["input"],
    output_names=["output"],
    dynamic_axes={
        "input":  {0: "batch_size"},
        "output": {0: "batch_size"}
    },
    opset_version=14,
)
print("ONNX model saved to lstm_autoencoder.onnx")


with open("lstm_threshold.txt", "w") as f:
    f.write(str(threshold))
print(f"Threshold saved to lstm_threshold.txt ({threshold:.6f})")

import onnxruntime as ort
sess = ort.InferenceSession("lstm_autoencoder.onnx")
test = np.random.randn(1, 10, 4).astype(np.float32)
result = sess.run(None, {"input": test})
print(f"ONNX verification completed: output shape = {result[0].shape}")
