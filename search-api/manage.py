# -*- coding: utf-8 -*-

from sqlalchemy.orm.mapper import configure_mappers

from api import create_app
from api.extensions import db
from api.user import Users, ADMIN, USER, ACTIVE
from api.tasks import MyTaskModel

application = create_app()


@application.cli.command("initdb")
def initdb():
    """Init/reset database."""

    db.drop_all()
    configure_mappers()
    db.create_all()

    admin = Users(name='Admin',
                  email=u'dmodi@devansh.in',
                  password=u'Qwerty1234',
                  role_code=ADMIN,
                  status_code=ACTIVE)

    db.session.add(admin)

    for i in range(1, 2):
        user = Users(name='Demo User',
                     email=u'demo@your-mail.com',
                     password=u'demopassword',
                     role_code=USER,
                     status_code=ACTIVE)
        db.session.add(user)

    for i in range(1, 5):
        _task = MyTaskModel(task="Task Random Number ## " + str(i), users_id=2)

        db.session.add(_task)

    db.session.commit()

    print("Database initialized with 2 users (admin, demo)")
