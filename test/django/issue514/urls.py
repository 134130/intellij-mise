from django.urls import path

from envcheck.views import index

urlpatterns = [
    path("", index, name="index"),
]
