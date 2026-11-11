"""Fast offline checks before Android Gradle Plugin compilation."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

base = Path(__file__).resolve().parents[1]
manifest = ET.parse(base / "app/src/main/AndroidManifest.xml").getroot()
ns = "{http://schemas.android.com/apk/res/android}"
activities = manifest.findall("./application/activity")
assert any(
    category.attrib.get(ns + "name") == "android.intent.category.HOME"
    for activity in activities
    for category in activity.findall("./intent-filter/category")
), "Folio must declare HOME launcher support"
assert all(activity.attrib.get(ns + "exported") == "true" for activity in activities), "Launcher must be exported"
source = (base / "app/src/main/java/org/folio/home/HomeActivity.java").read_text()
for marker in (
    "CONFIG_WIDGET = 73",
    "request==CONFIG_WIDGET",
    "configureWidget(id)",
    "cancelPendingWidget()",
    "onSaveInstanceState(Bundle out)",
    "widgetHost.deleteAppWidgetId",
    "new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)",
):
    assert marker in source, f"Missing widget lifecycle step: {marker}"
assert re.search(r"request==CONFIG_WIDGET\)\{setWidget\(id\)", source), "Configuration result must finish widget setup"
assert "request==BIND_WIDGET){\n                if(widgetManager.getAppWidgetInfo(id)!=null)configureWidget(id)" in source, "Binding must lead to configuration, not immediate widget display"
assert source.count("{") == source.count("}"), "Java braces unbalanced"
ET.parse(base / "app/src/main/res/values/styles.xml")
ET.parse(base / "app/src/main/res/drawable/folio_icon.xml")
print("PASS: launcher manifest, widget flow, and XML structure")
