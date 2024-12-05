
import numpy as np
import os
import random
import shutil

import torch
from OmniScaleCNN.Classifiers.OS_CNN.OS_CNN_easy_use import OS_CNN_easy_use
import numpy as np

from OmniScaleCNN.Classifiers.OS_CNN.OS_CNN_Structure_build import generate_layer_parameter_list

from config import BATCH_SIZE, EPOCHS, ES_PATIENCE

'''
This code contains a customized version of the OS_CNN_easy_use.py file from the OmniScaleCNN library.
'''

class OSCNN:

    # build the model
    def __init__(self, dataset_name : str = "", result_log_folder : str = "results/_tmp/", max_epoch : int = EPOCHS, device : str = "cuda:0") -> None:
        self.result_log_folder = result_log_folder
        self.dtype = torch.float
        self.device = torch.device(device)

        # create the _tmp if it does not exist
        if not os.path.exists(self.result_log_folder):
            os.makedirs(self.result_log_folder)

        self.model = OS_CNN_easy_use(
            Result_log_folder = self.result_log_folder, # the Result_log_folder,
            #dataset_name = dataset_name,           # dataset_name for creat log under Result_log_folder,
            device = device,                # Gpu 
            batch_size=BATCH_SIZE,
            max_epoch = max_epoch,                        # In our expirement the number is 2000 for keep it same with FCN for the example dataset 500 will be enough,
            paramenter_number_of_layer_list = [8*128, 5*128*256 + 2*256*128],
            print_result_every_x_epoch = 1
            )
        print("OSCNN Classifier built")
        
    # training of the model
    def fit(self, X_train, y_train, X_val, y_val, earlystopping = True, es_patience = ES_PATIENCE) -> None:
        if earlystopping:
            print(f"Early stopping enabled for OS_CNN with es_patience of {es_patience} epochs")
        
        # print the shapes of the train and validation sets
        print(f"X_train shape: {X_train.shape}")
        print(f"y_train shape: {y_train.shape}")
        print(f"X_val shape: {X_val.shape}")
        print(f"y_val shape: {y_val.shape}")

        self.model.fit(X_train, y_train, X_val, y_val, earlystopping=earlystopping, es_patience=es_patience)

    # return predictions for X_test
    def predict(self, X_test):
        y_pred = self.model.predict(X_test)
        return y_pred

    # return predictions of label 1 for X_test
    def predict_proba(self, X_test):
        y_pred = self.model.predict_proba(X_test)
        return y_pred[:, 1]

    def predict_sample(self, sample):
        y_pred = self.model.predict_sample(sample)
        return y_pred[:, 1]
    
    # save the model to path
    def dump(self, path : str) -> None:
        # copy model to results folder
        shutil.copy(self.model.model_save_path, path + '.pt')

    # initialization of OS CNN requires X and y data
    def initialize_CNN(self, X_train, y_train, X_test, y_test, device : str):
        self.device = torch.device(device if (torch.cuda.is_available() or torch.backends.mps.is_available()) else "cpu")
        print('[Initialization of CNN] code is running on: ', device)  

        X_train = torch.from_numpy(X_train)
        X_train.requires_grad = False
        X_train = X_train.to(device)
        X_train = X_train.unsqueeze_(1)
        y_train = torch.from_numpy(y_train).to(device)

        X_test = torch.from_numpy(X_test)
        X_test.requires_grad = False
        X_test = X_test.to(device)
        X_test = X_test.unsqueeze_(1)
        y_test = torch.from_numpy(y_test).to(device)
        # input_shape = X_train.shape[-1]
        self.n_class = max(y_train) + 1

        #net parameter
        paramenter_number_of_layer_list = [8*128, 5*128*256 + 2*256*128] 
        Max_kernel_size = 89

        # calcualte network structure
        receptive_field_shape= min(int(X_train.shape[-1]/4),Max_kernel_size)
        print('the shape of inpute data is:',X_train.shape)
        print('the max size of kernel is:', receptive_field_shape)
        self.layer_parameter_list = generate_layer_parameter_list(1,receptive_field_shape,paramenter_number_of_layer_list)
        print(self.layer_parameter_list)
        

    def load(self, path : str, script : bool = False):
        # self.model = torch.jit.load(path) if script else self.model.OS_CNN.load_state_dict(torch.load(path))
        model = torch.load(path + '.pt')
        model.eval()
        self.model.OS_CNN = model

    def set_seeds(self, seed : int = 42):
        torch.manual_seed(seed)
        np.random.seed(seed)
        random.seed(seed)
        torch.backends.cudnn.deterministic = True
        torch.backends.cudnn.benchmark = False