from appium.webdriver.common.appiumby import AppiumBy

PACKAGE = "net.red5.testbed"


class SettingsPage:
    def __init__(self, driver):
        self.driver = driver

    def _scroll_and_find(self, res_id):
        """Scroll the settings screen until the field is visible, then return it."""
        return self.driver.find_element(
            AppiumBy.ANDROID_UIAUTOMATOR,
            f'new UiScrollable(new UiSelector().scrollable(true))'
            f'.scrollIntoView(new UiSelector().resourceId("{PACKAGE}:id/{res_id}"))',
        )

    def _set_field(self, res_id, value):
        el = self._scroll_and_find(res_id)
        el.clear()
        el.send_keys(value)

    def set_license_key(self, value):
        self._set_field("et_license_key", value)

    def set_stream_manager_host(self, value):
        self._set_field("et_stream_manager_host", value)

    def set_standalone_server_ip(self, value):
        self._set_field("et_standalone_server_ip", value)

    def set_app_name(self, value):
        self._set_field("et_app_name", value)

    def set_stream_name(self, value):
        self._set_field("et_stream_name", value)

    def set_username(self, value):
        self._set_field("et_username", value)

    def set_password(self, value):
        self._set_field("et_password", value)

    def save(self):
        self.driver.back()
