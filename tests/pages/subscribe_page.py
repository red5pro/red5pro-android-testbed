from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

PACKAGE = "net.red5.testbed"


class SubscribePage:
    def __init__(self, driver, timeout=30):
        self.driver = driver
        self.wait = WebDriverWait(driver, timeout)

    def _id(self, name):
        return f"{PACKAGE}:id/{name}"

    def tap_subscribe(self):
        self.wait.until(
            EC.element_to_be_clickable((AppiumBy.ID, self._id("btn_subscribe")))
        ).click()

    def wait_for_live(self):
        WebDriverWait(self.driver, 20).until(
            lambda d: d.find_element(
                AppiumBy.ID, self._id("status_indicator_text")
            ).text == "Live"
        )

    def stop_subscribe(self):
        self.wait.until(
            EC.element_to_be_clickable((AppiumBy.ID, self._id("btn_subscribe")))
        ).click()

    def wait_for_disconnected(self):
        WebDriverWait(self.driver, 15).until(
            lambda d: d.find_element(
                AppiumBy.ID, self._id("status_indicator_text")
            ).text == "Disconnected"
        )

    def get_status(self):
        return self.driver.find_element(AppiumBy.ID, self._id("status_indicator_text")).text

    def get_subscribe_button_text(self):
        return self.driver.find_element(AppiumBy.ID, self._id("btn_subscribe")).text
