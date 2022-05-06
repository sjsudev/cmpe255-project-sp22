# -*- coding: utf-8 -*-

from flask import Flask, json, request

import time

from .config import DefaultConfig
from .user import Users, UsersAdmin
from .settings import settings
from .tasks import tasks, MyTaskModelAdmin
from .frontend import frontend, ContactUsAdmin
from .extensions import db, mail, cache, login_manager, admin
from .utils import INSTANCE_FOLDER_PATH, pretty_date
import uuid
#import json

# For import *
__all__ = ['create_app']

app = Flask(__name__)

DEFAULT_BLUEPRINTS = (
    frontend,
    settings,
    tasks
)


def create_app(config=None, app_name=None, blueprints=None):
    # Create a Flask app

    if app_name is None:
        app_name = DefaultConfig.PROJECT
    if blueprints is None:
        blueprints = DEFAULT_BLUEPRINTS

    app = Flask(app_name,
                instance_path=INSTANCE_FOLDER_PATH,
                instance_relative_config=True)

    configure_app(app, config)
    configure_hook(app)
    configure_blueprints(app, blueprints)
    configure_extensions(app)
    configure_logging(app)
    configure_template_filters(app)
    configure_error_handlers(app)
    configure_inference_handlers(app)

    return app


def configure_app(app, config=None):
    # Different ways of configurations i.e local or production

    app.config.from_object(DefaultConfig)

    app.config.from_pyfile('production.cfg', silent=True)

    if config:
        app.config.from_object(config)


def configure_extensions(app):

    # flask-sqlalchemy
    db.init_app(app)

    # flask-mail
    mail.init_app(app)

    # flask-cache
    cache.init_app(app)

    # flask-admin
    admin.add_view(ContactUsAdmin(db.session))
    admin.add_view(UsersAdmin(db.session))
    admin.add_view(MyTaskModelAdmin(db.session))
    admin.init_app(app)

    @login_manager.user_loader
    def load_user(id):
        return Users.query.get(id)
    login_manager.setup_app(app)


def configure_blueprints(app, blueprints):
    # Configure blueprints in views

    for blueprint in blueprints:
        app.register_blueprint(blueprint)


def configure_template_filters(app):

    @app.template_filter()
    def _pretty_date(value):
        return pretty_date(value)

    @app.template_filter()
    def format_date(value, format='%Y-%m-%d'):
        return value.strftime(format)


def configure_logging(app):
    # Configure logging

    if app.debug:
        # Skip debug and test mode. Better check terminal output.
        return

    # TODO: production loggers for (info, email, etc)


def configure_hook(app):
    @app.before_request
    def before_request():
        pass


def configure_error_handlers(app):

    @app.errorhandler(403)
    def forbidden_page(error):
        return "Oops! You don't have permission to access this page.", 403

    @app.errorhandler(404)
    def page_not_found(error):
        return "Opps! Page not found.", 404

    @app.errorhandler(500)
    def server_error_page(error):
        return "Oops! Internal server error. Please try after sometime.", 500

def make_summary():
    # model inference here
    # model.eval()

    return {
        'prediction_id': uuid.uuid4(),
        'classifiers': ['L-Shaped', 'Stone Countertop']
    }

def configure_inference_handlers(app):
    @app.route('/predict', methods=['POST'])
    def predict():
        if 'image1' not in request.files:
            return 'there is no image1 in the request!'
        
        file1 = request.files['image1']
        time.sleep(4)
        # images, labels, probs = get_predictions(model_ft, dataloaders['test'], device)

        data = make_summary()
        response = app.response_class(
            response=json.dumps(data),
            status=200,
            mimetype='application/json'
        )
        return response
