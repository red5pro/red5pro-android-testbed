from appium import webdriver
from appium.options.android.uiautomator2.base import UiAutomator2Options


def create_android_driver(config):
    opts = UiAutomator2Options()
    opts.udid = config["android"]["udid"]
    opts.app_package = config["android"]["app_package"]
    opts.app_activity = config["android"]["app_activity"]
    opts.no_reset = True
    opts.auto_grant_permissions = True
    return webdriver.Remote(config["android"]["appium_url"], options=opts)
