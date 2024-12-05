from flask import Flask, request, jsonify
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import load_model

from setup import load_models
from config import SERVICE_PORT

app = Flask(__name__)

models = load_models()

def basic_validation(data):
    if data.shape[-1] != 100:
        return jsonify({"error": "Invalid input length. Expected length: 100"}), 400
    
    return None

def preprocess(data):
    window = np.array(data).squeeze()
    window = window.reshape(1, len(window))

    # Scale values without losing precision
    window = window * 10**6
    # Standardize data
    window = (window - window.mean()) / window.std()
    # Handle cases where std is equal to 0
    window[np.isnan(window) | np.isinf(window)] = 0

    return window

# Define predict endpoint for FCN model
@app.route("/predict/fcn", methods=["POST"])
def predict_fcn():
    data = request.get_json()
    data = preprocess(data)

    validation_error = basic_validation(data)
    if validation_error:
        return validation_error

    prediction = models["FCN"].predict(data)[0]
    return jsonify({"steady": bool(prediction)})

# Define predict endpoint for OSCNN model
@app.route("/predict/oscnn", methods=["POST"])
def predict_oscnn():
    data = request.get_json()
    data = preprocess(data)

    validation_error = basic_validation(data)
    if validation_error:
        return validation_error
    
    if data.dtype != np.float64:
        data = data.astype(np.float64)  
    
    prediction = models["OSCNN"].predict(data)[0]
    return jsonify({"steady": bool(prediction)})

# Define predict endpoint for Rocket model
@app.route("/predict/rocket", methods=["POST"])
def predict_rocket():
    data = request.get_json()
    data = preprocess(data)
    
    validation_error = basic_validation(data)
    if validation_error:
        return validation_error
    
    prediction = models["Rocket"].predict(data)[0]
    return jsonify({"steady": bool(prediction)})

# Default route for checking service
@app.route("/", methods=["GET"])
def health_check():
    return jsonify({"status": "Service running"}), 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=SERVICE_PORT)