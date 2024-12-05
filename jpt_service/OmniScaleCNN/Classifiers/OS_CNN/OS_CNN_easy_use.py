import datetime
import os
from sklearn.metrics import accuracy_score
from os.path import dirname
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import DataLoader
from torch.utils.data import TensorDataset

from .OS_CNN_Structure_build import generate_layer_parameter_list
from .log_manager import eval_condition, eval_model, save_to_log
from .OS_CNN import OS_CNN

from config import EPOCHS, ES_PATIENCE, ROP_PATIENCE, BATCH_SIZE


class OS_CNN_easy_use():
    
    def __init__(self,Result_log_folder, 
                 device, 
                 dataset_name="", 
                 start_kernel_size = 1,
                 Max_kernel_size = 89, 
                 paramenter_number_of_layer_list = [8*128, 5*128*256 + 2*256*128], 
                 max_epoch = EPOCHS, 
                 batch_size=BATCH_SIZE,
                 print_result_every_x_epoch = 50,
                 lr = 0.001
                ):
        
        super(OS_CNN_easy_use, self).__init__()
        
        if not os.path.exists(Result_log_folder +dataset_name+'/'):
            os.makedirs(Result_log_folder +dataset_name+'/')
        #Initial_model_path = Result_log_folder +dataset_name+'/'+dataset_name+'initial_model'
        model_save_path = Result_log_folder +dataset_name+'/'+dataset_name+'oscnn.pt'
        

        self.Result_log_folder = Result_log_folder
        self.dataset_name = dataset_name        
        self.model_save_path = model_save_path
        #self.Initial_model_path = Initial_model_path
        
        # Fix for Mac M1
        print("Building OSCNN easy use")
        self.device = torch.device(device if torch.cuda.is_available() else "cpu") ## ORIGINAL SETTING
        # self.device = torch.device(device if (torch.cuda.is_available() or torch.backends.mps.is_available()) else "cpu") # you should check that Torch has been compiled with CUDA enabled

        self.start_kernel_size = start_kernel_size
        self.Max_kernel_size = Max_kernel_size
        self.paramenter_number_of_layer_list = paramenter_number_of_layer_list
        self.max_epoch = max_epoch
        self.batch_size = batch_size
        self.print_result_every_x_epoch = print_result_every_x_epoch
        self.lr = lr
        self.OS_CNN = None
    
    def build_ONN_to_load(self, X_train, y_train, X_val, y_val):

        print('code is running on ',self.device)
        
        
        # covert numpy to pytorch tensor and put into gpu
        X_train = torch.from_numpy(X_train)
        X_train.requires_grad = False
        X_train = X_train.to(self.device)
        y_train = torch.from_numpy(y_train).to(self.device)
        
        
        X_test = torch.from_numpy(X_val)
        X_test.requires_grad = False
        X_test = X_test.to(self.device)
        y_test = torch.from_numpy(y_val).to(self.device)
        
        
        # add channel dimension to time series data
        if len(X_train.shape) == 2:
            X_train = X_train.unsqueeze_(1)
            X_test = X_test.unsqueeze_(1)

        input_shape = X_train.shape[-1]
        n_class = max(y_train) + 1
        receptive_field_shape= min(int(X_train.shape[-1]/4),self.Max_kernel_size)
        
        # generate parameter list
        layer_parameter_list = generate_layer_parameter_list(self.start_kernel_size,
                                                             receptive_field_shape,
                                                             self.paramenter_number_of_layer_list,
                                                             in_channel = int(X_train.shape[1]))
        
        
        torch_OS_CNN = OS_CNN(layer_parameter_list, n_class.item(), False).to(self.device)

        return torch_OS_CNN
        
    def __validation_loss_with_dataset(self, model, val_set):
        criterion = nn.CrossEntropyLoss()
        # get X set from TensorDataset
        X_val = val_set.tensors[0]
        y_val = val_set.tensors[1]

        y_predict = model(X_val)

        # y_true_t = torch.from_numpy(y_val).long()
        # y_pred_t = torch.from_numpy(y_predict).float()

        val_loss = criterion(y_predict, y_val)

        return val_loss.item()
    
    def __val_loss(self, model, val_loader):
        criterion = nn.CrossEntropyLoss()
        y_true = []
        y_pred = []
        # get X set from TensorDataset
        with torch.no_grad():
            for X, y in val_loader:
                y_pred.append(model(X))
                y_true.append(y)
            
            y_true = torch.cat(y_true, dim=0)
            y_pred = torch.cat(y_pred, dim=0)
            
            val_loss = criterion(y_pred, y_true)
            return val_loss.item()
                
    def fit(self, X_train, y_train, X_val, y_val, earlystopping : bool = 1, es_patience : int = ES_PATIENCE, verbose : bool = 0):

        print('code is running on ',self.device)
        
        # covert numpy to pytorch tensor and put into gpu
        X_train = torch.from_numpy(X_train)
        X_train.requires_grad = False
        X_train = X_train.to(self.device)
        y_train = torch.from_numpy(y_train).to(self.device)
        
        
        X_test = torch.from_numpy(X_val)
        X_test.requires_grad = False
        X_test = X_test.to(self.device)
        y_test = torch.from_numpy(y_val).to(self.device)
        
        
        # add channel dimension to time series data
        if len(X_train.shape) == 2:
            X_train = X_train.unsqueeze_(1)
            X_test = X_test.unsqueeze_(1)

        input_shape = X_train.shape[-1]
        n_class = max(y_train) + 1
        receptive_field_shape= min(int(X_train.shape[-1]/4),self.Max_kernel_size)
        
        # generate parameter list
        layer_parameter_list = generate_layer_parameter_list(self.start_kernel_size,
                                                             receptive_field_shape,
                                                             self.paramenter_number_of_layer_list,
                                                             in_channel = int(X_train.shape[1]))
        
        
        torch_OS_CNN = OS_CNN(layer_parameter_list, n_class.item(), False).to(self.device)
        
        # save_initial_weight
#        torch.save(torch_OS_CNN, self.Initial_model_path)
        
        
        # loss, optimizer, scheduler
        criterion = nn.CrossEntropyLoss()
        optimizer = optim.Adam(torch_OS_CNN.parameters(),lr= self.lr)
        scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, 'min', factor=0.5, patience=ROP_PATIENCE, min_lr=0.0001)
        
        # build dataloader
        
        train_dataset = TensorDataset(X_train, y_train)
        train_loader = DataLoader(train_dataset, batch_size=max(int(min(X_train.shape[0] / 10, self.batch_size)),2), shuffle=True)
        test_dataset = TensorDataset(X_test, y_test)
        test_loader = DataLoader(test_dataset, batch_size=max(int(min(X_train.shape[0] / 10, self.batch_size)),2), shuffle=False)
        
        
        torch_OS_CNN.train()   

        # earlystopping initialization
        previous_metric = -1
        best_val_loss = np.inf
        not_improving_count = 0
        
        for i in range(self.max_epoch):
            for sample in train_loader:
                optimizer.zero_grad()
                y_predict = torch_OS_CNN(sample[0])
                output = criterion(y_predict, sample[1])
                output.backward()
                optimizer.step()
            

            # set model to eval mode and evaluate
            torch_OS_CNN.eval()

            # earlystopping
            current_val_loss = self.__val_loss(torch_OS_CNN, test_loader)
            #current_val_loss = self.__validation_loss_with_dataset(torch_OS_CNN, test_dataset)
            # current_val_loss = self.__val_loss_with_dataset(torch_OS_CNN, test_dataset)

            # print("Validation loss averaging = ", current_val_loss)
            # print("Validation loss like training = ", self.__val_loss_clone(torch_OS_CNN, test_loader))

            scheduler.step(current_val_loss) # use this line if you want to use ReduceLROnPlateau scheduler on val_loss
            # scheduler.step(output) # use this line if you want to use ReduceLROnPlateau scheduler on loss

            if earlystopping:
                if current_val_loss < best_val_loss:
                    best_val_loss = current_val_loss
                    not_improving_count = 0
                    # model checkpoint
                    torch.save(torch_OS_CNN, self.model_save_path)
                else:
                    not_improving_count += 1
                    print('not improving count = ', not_improving_count)
                    if not_improving_count >= es_patience:
                        print('Early stopping at epoch', i)
                        print(f"last {es_patience} epoches are not improving: ")
                        break
            
            acc_train = eval_model(torch_OS_CNN, train_loader)
            acc_test = eval_model(torch_OS_CNN, test_loader)

            # set model back to train mode
            # Rationale: BatchNorm and Dropout behave differently in train and eval mode
            torch_OS_CNN.train()

            # print result each self.print_result_every_x_epoch epochs
            if eval_condition(i,self.print_result_every_x_epoch):
                for param_group in optimizer.param_groups:
                    print('epoch =',i, 'lr = ', param_group['lr'])
                
                print(datetime.datetime.now())
                print('train_acc=\t', acc_train, '\t test_acc=\t', acc_test, '\t loss=\t', str(output.item()), '\t val loss=\t', str(current_val_loss))

                # print("comparing acc_test with previous_metric", acc_test, previous_metric)

                sentence = 'epoch=\t' + str(i) + ' train_acc=\t'+str(acc_train) + '\t test_acc=\t' + str(acc_test) + '\t loss=\t' + str(output.item()) + '\t val loss=\t' + str(current_val_loss)
                
                if verbose:
                    print('log saved at:')
                
                save_to_log(sentence,self.Result_log_folder, self.dataset_name, verbose=verbose)
                # torch.save(torch_OS_CNN.state_dict(), self.model_save_path)
         
        # torch.save(torch_OS_CNN.state_dict(), self.model_save_path)
        self.OS_CNN = torch_OS_CNN
 
    def predict(self, X_test):
        
        X_test = torch.from_numpy(X_test)
        X_test.requires_grad = False
        X_test = X_test.to(self.device)
        
        if len(X_test.shape) == 2:
            X_test = X_test.unsqueeze_(1)
        
        test_dataset = TensorDataset(X_test)
        test_loader = DataLoader(test_dataset, batch_size=max(int(min(X_test.shape[0] / 10, self.batch_size)),2), shuffle=False)

        # print("Test load head", list(test_loader.data.numpy())[:10])
        print("Test array", max(int(min(X_test.shape[0] / 10, self.batch_size)),2), next(iter(test_loader))[0].cpu().numpy().shape, test_dataset.__len__())
        
        self.OS_CNN.eval()
        
        predict_list = np.array([])
        for sample in test_loader:
            y_predict = self.OS_CNN(sample[0])
            y_predict = y_predict.detach().cpu().numpy()
            y_predict = np.argmax(y_predict, axis=1)
            predict_list = np.concatenate((predict_list, y_predict), axis=0)
            
        return predict_list
        
    def predict_proba(self, X_test):

        X_test = torch.from_numpy(X_test)
        X_test.requires_grad = False
        X_test = X_test.to(self.device)
        
        if len(X_test.shape) == 2:
            X_test = X_test.unsqueeze_(1)
        
        test_dataset = TensorDataset(X_test)
        test_loader = DataLoader(test_dataset, batch_size=max(int(min(X_test.shape[0] / 10, self.batch_size)),2), shuffle=False)

        # print("Test load head", list(test_loader.data.numpy())[:10])
        print("Test array", max(int(min(X_test.shape[0] / 10, self.batch_size)),2), next(iter(test_loader))[0].cpu().numpy().shape, test_dataset.__len__())
        
        self.OS_CNN.eval()
        
        predict_list = np.empty((0, 2)) # Define an empty array given the shape (0, NUM_LABELS)
        for sample in test_loader:
            # get logits
            y_predict = self.OS_CNN(sample[0])
            y_predict = y_predict.detach().cpu() #.numpy()
            # calc probs
            # y_probs = torch.nn.functional.softmax(y_predict, dim = 1)
            y_probs = torch.sigmoid(y_predict)
            # y_predict = np.argmax(y_predict, axis=1)
            predict_list = np.concatenate((predict_list, y_probs), axis=0)
            
        return predict_list
    
    def predict_sample(self, X_test):
        X_test = X_test.reshape((1, 1, 100))
        X_test = torch.from_numpy(X_test)
        X_test.requires_grad = False
        X_test = X_test.to(self.device).float()

        self.OS_CNN.eval()

        y_predict = self.OS_CNN(X_test)
        y_predict = y_predict.detach().cpu()
        y_probs = torch.sigmoid(y_predict).numpy()

        return y_probs