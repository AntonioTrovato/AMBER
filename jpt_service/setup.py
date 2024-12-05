from datetime import datetime

from config import FCN_PATH, OSCNN_PATH, Rocket_PATH

from wrap_fcn import FCN
from wrap_oscnn import OSCNN
from wrap_rocket import Rocket


def load_models():

    models = {}
    # Load your pre-trained Keras model
    fcn_mdl = FCN()
    fcn_mdl.load(FCN_PATH)
    models["FCN"] = fcn_mdl
    print(f"[{datetime.now()}] Model {FCN_PATH} loaded ...")

    oscnn_mdl = OSCNN()
    oscnn_mdl.load(OSCNN_PATH)
    models["OSCNN"] = fcn_mdl
    print(f"[{datetime.now()}] Model {OSCNN_PATH} loaded ...")

    rkt_mdl = Rocket()
    rkt_mdl.load(Rocket_PATH)
    models["Rocket"] = fcn_mdl
    print(f"[{datetime.now()}] Model {Rocket_PATH} loaded ...")

    return models