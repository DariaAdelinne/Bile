"""
weather_gui.py - interfata grafica PyQt5 a aplicatiei de monitorizare a vremei.

Comunica prin TCP cu VisualizationService (vezi weather_client.py). Afiseaza vremea curenta pentru
un oras (inclusiv CARE replica a raspuns, ca sa se vada modelul de replicare) si istoricul
elementelor citite.

Rulare:  python weather_gui.py
Necesita:  pip install PyQt5   (pe Debian, daca lipseste: sudo apt install python3-pyqt5)
"""
import sys

from PyQt5.QtWidgets import (
    QApplication, QWidget, QLabel, QLineEdit, QPushButton, QVBoxLayout, QHBoxLayout,
    QListWidget, QGroupBox, QFormLayout, QMessageBox
)
from PyQt5.QtCore import Qt

import weather_client


class WeatherWindow(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Monitorizare vreme - replicare + vizualizare (SD)")
        self.resize(560, 480)
        self._build_ui()

    def _build_ui(self):
        layout = QVBoxLayout(self)

        # --- rand de cautare ---
        search = QHBoxLayout()
        search.addWidget(QLabel("Oras:"))
        self.city_input = QLineEdit()
        self.city_input.setPlaceholderText("ex: Bucuresti, Cluj, London, Paris...")
        self.city_input.returnPressed.connect(self.on_get_weather)
        search.addWidget(self.city_input)
        self.btn_get = QPushButton("Vezi vremea")
        self.btn_get.clicked.connect(self.on_get_weather)
        search.addWidget(self.btn_get)
        layout.addLayout(search)

        # --- panou vreme curenta ---
        box = QGroupBox("Vremea curenta")
        form = QFormLayout()
        self.lbl_city = QLabel("-")
        self.lbl_temp = QLabel("-")
        self.lbl_wind = QLabel("-")
        self.lbl_desc = QLabel("-")
        self.lbl_time = QLabel("-")
        self.lbl_replica = QLabel("-")
        form.addRow("Locatie:", self.lbl_city)
        form.addRow("Temperatura:", self.lbl_temp)
        form.addRow("Vant:", self.lbl_wind)
        form.addRow("Conditii:", self.lbl_desc)
        form.addRow("Ora:", self.lbl_time)
        form.addRow("Raspuns de la:", self.lbl_replica)
        box.setLayout(form)
        layout.addWidget(box)

        # --- istoric ---
        layout.addWidget(QLabel("Istoric elemente citite:"))
        self.history_list = QListWidget()
        layout.addWidget(self.history_list)

        btns = QHBoxLayout()
        self.btn_hist = QPushButton("Reincarca istoric")
        self.btn_hist.clicked.connect(self.refresh_history)
        btns.addWidget(self.btn_hist)
        layout.addLayout(btns)

        self.status = QLabel("Gata. Conectare la VisualizationService localhost:%d" % weather_client.DEFAULT_PORT)
        self.status.setStyleSheet("color: gray;")
        layout.addWidget(self.status)

    def on_get_weather(self):
        city = self.city_input.text().strip()
        if not city:
            return
        try:
            data, raw = weather_client.query(city)
        except Exception as e:
            QMessageBox.critical(self, "Eroare conexiune",
                                 "Nu ma pot conecta la VisualizationService.\n\n%s" % e)
            self.status.setText("Eroare: %s" % e)
            return

        if data is None:
            self.status.setText("Raspuns: %s" % raw)
            QMessageBox.warning(self, "Negasit", "Nu am putut obtine vremea: %s" % raw)
            return

        self.lbl_city.setText("%s (%s)" % (data["city"], data["country"]))
        self.lbl_temp.setText("%s °C" % data["temperature"])
        self.lbl_wind.setText("%s km/h" % data["windspeed"])
        self.lbl_desc.setText(data["description"])
        self.lbl_time.setText(data["time"])
        self.lbl_replica.setText("Replica #%s   (sursa: %s)" % (data["replica"], data["source"]))
        self.status.setText("OK - element citit pentru %s" % data["city"])
        self.refresh_history()

    def refresh_history(self):
        try:
            items = weather_client.history()
        except Exception as e:
            self.status.setText("Eroare istoric: %s" % e)
            return
        self.history_list.clear()
        for d in items:
            self.history_list.addItem(
                "%s (%s): %s °C, %s  [replica #%s, %s]"
                % (d["city"], d["country"], d["temperature"], d["description"], d["replica"], d["source"])
            )


def main():
    app = QApplication(sys.argv)
    win = WeatherWindow()
    win.show()
    sys.exit(app.exec_())


if __name__ == "__main__":
    main()
