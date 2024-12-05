from aeon.classification.convolution_based import RocketClassifier
import numpy as np

from config import BATCH_SIZE

class Rocket:

    def __init__(self, num_kernels : int = 500):
        self.classifier = RocketClassifier(num_kernels = num_kernels)

        print("RocketClassifier built")
        
    def fit(self, X_train, y_train):
        self.classifier.fit(X_train, y_train)

    def predict(self, X_test, batch_size=BATCH_SIZE):
        # Calculate the number of batches
        n_batches = int(np.ceil(X_test.shape[0] / batch_size))

        # Create an empty list to store the batch-wise predictions
        y_preds = []

        # Loop over each batch and append predictions to y_preds
        for i in range(n_batches):
            start_idx = i * batch_size
            end_idx = (i + 1) * batch_size
            y_pred_batch = self.classifier.predict(X_test[start_idx:end_idx])
            y_preds.append(y_pred_batch)

        # Concatenate all batch predictions to get the final y_pred array
        y_pred = np.concatenate(y_preds, axis=0)

        return y_pred

    def predict_proba(self, X_test, batch_size=BATCH_SIZE):
        # Calculate the number of batches
        n_batches = int(np.ceil(X_test.shape[0] / batch_size))

        # Create an empty list to store the batch-wise predictions
        y_preds = []

        # Loop over each batch and append predictions to y_preds
        for i in range(n_batches):
            start_idx = i * batch_size
            end_idx = (i + 1) * batch_size
            y_pred_batch = self.classifier.predict_proba(X_test[start_idx:end_idx])
            y_preds.append(y_pred_batch)

        # Concatenate all batch predictions to get the final y_pred array
        y_pred = np.concatenate(y_preds, axis=0)

        return y_pred[:, 1]
    
    def predict_sample(self, sample):
        return self.classifier.predict_proba(sample)[:, 1]
    
    def dump(self, path : str):
        self.classifier.save(path)

    def load(self, path : str):
        self.classifier = RocketClassifier.load_from_path(path + ".zip")

    # set seed for reproducibility
    def set_seeds(self, seed : int = 42):
        np.random.seed(seed)