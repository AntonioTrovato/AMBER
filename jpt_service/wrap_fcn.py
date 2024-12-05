import json
import os
import shutil

from tensorflow import keras
import tensorflow as tf
import numpy as np
import random as rn
import os

from config import BATCH_SIZE, EPOCHS, ES_PATIENCE, ROP_PATIENCE

class FCN:
    metrics = [ keras.metrics.TruePositives(name='tp'),
            keras.metrics.FalsePositives(name='fp'),
            keras.metrics.TrueNegatives(name='tn'),
            keras.metrics.FalseNegatives(name='fn'),
            keras.metrics.BinaryAccuracy(name='accuracy'),
            keras.metrics.Precision(name='precision'),
            keras.metrics.Recall(name='recall'),
            keras.metrics.AUC(name='auc'),
            keras.metrics.AUC(name='prc', curve='PR')]

    def __init__(self, model=None) -> None:
        self.model = model
        # create _tmp folder if not exists
        dir = "./results/_tmp"
        if not os.path.exists(dir):
            os.makedirs(dir)
        self.model_save_path = dir + "/fcn.h5"
        print("FCN Classifier built")

    
    def _make_model(self, input_shape):

        input_layer = keras.layers.Input(input_shape)

        conv1 = keras.layers.Conv1D(filters=128, kernel_size=8, padding="same")(input_layer)
        conv1 = keras.layers.BatchNormalization()(conv1)
        conv1 = keras.layers.ReLU()(conv1)

        conv2 = keras.layers.Conv1D(filters=256, kernel_size=5, padding="same")(conv1)
        conv2 = keras.layers.BatchNormalization()(conv2)
        conv2 = keras.layers.ReLU()(conv2)

        conv3 = keras.layers.Conv1D(filters=128, kernel_size=3, padding="same")(conv2)
        conv3 = keras.layers.BatchNormalization()(conv3)
        conv3 = keras.layers.ReLU()(conv3)

        gap = keras.layers.GlobalAveragePooling1D()(conv3)

        output_layer = keras.layers.Dense(1, activation="sigmoid")(gap)

        return keras.models.Model(inputs=input_layer, outputs=output_layer)

    def fit(self, x_train, y_train, x_val, y_val, epochs=EPOCHS, batch_size=BATCH_SIZE):
        # reshape input
        x_train = x_train.reshape(x_train.shape[0], x_train.shape[1], 1)
        x_val = x_val.reshape(x_val.shape[0], x_val.shape[1], 1)

        # shuffle training data
        idx = np.random.permutation(len(x_train))
        x_train = x_train[idx]
        y_train = y_train[idx]

        # create model
        input_shape = x_train.shape[1:]
        self.model = self._make_model(input_shape)
        
        # define optimizer 
        optimizer=keras.optimizers.Adam(learning_rate=1e-03, beta_1=0.9, beta_2=0.999, epsilon=1e-08)
        
        # compile model
        self.model.compile(optimizer=optimizer, loss=keras.losses.BinaryCrossentropy(), metrics=self.__class__.metrics)
        
        # define early stopping
        callbacks = [ keras.callbacks.EarlyStopping(monitor="val_loss", mode='min', patience=ES_PATIENCE, verbose=1),
                      keras.callbacks.ReduceLROnPlateau(monitor="val_loss", mode='min', factor=0.5, patience=ROP_PATIENCE, min_lr=1e-4, verbose=1),
                      keras.callbacks.ModelCheckpoint(self.model_save_path, monitor="val_loss", mode="min", save_best_only = True, verbose=1)]

        # fit model
        history =  self.model.fit(x_train, y_train,
                            batch_size=batch_size, epochs=epochs, callbacks=callbacks,
                            validation_data=(x_val, y_val),
                            verbose=1)
        
        # create history folder if not exists
        if not os.path.exists("./results/history"):
            os.makedirs("./results/history")

        # dump history json
        with open("./results/history/fcn_history.json", "w") as f:
            metrics = ['loss'] +  [m.name for m in self.__class__.metrics]
            metrics += ["val_" + m for m in metrics]
            results = {m: history.history[m] for m in metrics}
            json.dump(results, f)

        return history


    def predict(self, x):
        x = x.reshape(x.shape[0], x.shape[1], 1)
        probas = self.model.predict(x, batch_size=BATCH_SIZE).reshape(-1)
        y = (probas > 0.5).astype(int)
        return y

    def predict_proba(self, x):
        x = x.reshape(x.shape[0], x.shape[1], 1)
        probas = self.model.predict(x, batch_size=BATCH_SIZE).reshape(-1)
        return probas
    
    def predict_sample(self, sample):
        # x = x.reshape(x.shape[0], x.shape[1], 1)
        probas = self.model(sample, training = False).numpy().reshape(-1)
        return probas

    def dump(self, path):
        # self.model.save(path) 
        # copy model to results folder
        shutil.copy(self.model_save_path, path + ".h5")

    def load(self, path):
        self.model = keras.models.load_model(path + ".h5")

    # set seeds for reproducibility
    def set_seeds(self, seed : int = 42):
        os.environ['PYTHONHASHSEED']=str(seed)
        np.random.seed(seed)
        rn.seed(seed)
        tf.random.set_seed(seed)