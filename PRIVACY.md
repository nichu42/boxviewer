# Legal Disclosures & Privacy Policy / Impressum & Datenschutzerklärung

**Effective Date / Stand:** June 25, 2026

*   [English Version (Imprint & Privacy Policy)](#english-version)
*   [Deutsche Version (Impressum & Datenschutzerklärung)](#deutsche-version)

---

<a id="english-version"></a>
# English Version: Imprint & Privacy Policy

<a id="imprint"></a>
## Part 1: Imprint (Provider Identification)

### Provider under § 5 DDG:
**Nicolai Roediger**  
c/o Block Services  
Stuttgarter Str. 106  
70736 Fellbach  
Germany  

### Contact & Support:
* **Email:** admin@42bit.email
* **Phone:** +49 1579 2516692 (urgent legal issues only)
* **Fax:** +49 6181 254505
* **Support & Issue Tracker:** [https://codeberg.org/nichu42/BoxViewer/issues](https://codeberg.org/nichu42/BoxViewer/issues)

### Dispute Resolution:
The European Commission provides a platform for online dispute resolution (ODR): [https://ec.europa.eu/consumers/odr](https://ec.europa.eu/consumers/odr).  
We are neither willing nor obliged to participate in dispute resolution proceedings before a consumer arbitration board.

### Warranty Disclaimer:
As this is a free, open-source software project distributed under the [GPLv3 license](https://codeberg.org/nichu42/BoxViewer/src/branch/main/LICENSE), the application and website are provided "as is", without warranty of any kind, express or implied. In no event shall the authors or copyright holders be liable for any claim, damages, or other liability arising from the use of the software.

---

<a id="privacy"></a>
## Part 2: Privacy Policy (GDPR)

BoxViewer is an open-source companion app for the openSenseMap platform. We value your privacy and are committed to maintaining a clean, tracking-free, and secure user experience. This Privacy Policy details how BoxViewer processes information.

### 1. No Collection or Transmission of Personal Data
BoxViewer does **not** collect, store, or transmit any personal information, usage analytics, telemetry, crash reporting, or diagnostic data to any external server. 
* All diagnostic logs and error reporting are kept **strictly local** on your device.
* No background analytics pings or telemetry SDKs (such as Firebase, Google Analytics, or Crashlytics) are integrated.

### 2. Network Connections
To provide its core functionality, BoxViewer only communicates with the official **openSenseMap API**:
* **API Host:** `https://api.opensensemap.org`
* **Purpose:** To fetch public senseBox configurations, metadata, and current sensor measurements.
* No other third-party servers, advertisement networks, or tracking systems are contacted.

### 3. Location Data
When you grant location permissions to BoxViewer:
* The app uses the standard, native Android `LocationManager` to determine your device's coordinates (via GPS, Network, or Passive location providers).
* **Local Processing:** This location is used solely on-device to calculate distances to nearby senseBoxes or to show your location on the discovery map.
* **No Sharing:** Your location coordinates are never sent to our servers, openSenseMap, or any other third parties.

### 4. Local SQLite Storage
The app stores your preferences, bookmarked senseBoxes, and cached sensor data locally on your device in a secure SQLite database. 
* This data is never synchronized with any cloud services.
* If you uninstall the app or clear its data in Android settings, all stored bookmarks, cached sensor values, and configurations are permanently deleted.

### 5. Website Hosting & Redirect Services
When you visit the BoxViewer landing and deep-link forwarding page (`https://nichu42.codeberg.page/BoxViewer`):
* **Server Log Files & Hosting:** The website is hosted on Codeberg Pages, operated by Codeberg e.V. (Berlin, Germany). When loading the page, your browser automatically transmits connection metadata (such as your IP address, browser type, operating system, referrer URL, and access timestamps) to Codeberg's servers. According to Codeberg's Privacy Policy, IP addresses are truncated when stored in log files so that they are not associated with your personal data, and are retained for no more than 7 days. This processing is technically necessary to serve the web pages securely and reliably (Art. 6 (1)(f) GDPR). For further details, please refer to the [Codeberg Privacy Policy](https://codeberg.org/Codeberg/org/src/branch/main/PrivacyPolicy.md).
* **No Web Tracking:** The website does not use cookies, local storage tracking keys, or third-party web analytics tools.
* **Local Redirect Script:** The redirect script extracts the senseBox ID entirely client-side inside your browser to open the application via deep link (`boxviewer://box/{id}`). No ID parameters or navigation history are sent to our servers.

### 6. Contact Info & Data Controller
BoxViewer is an open-source project. If you have any questions, feedback, or data privacy inquiries, you can contact the project maintainer and data controller:

**Nicolai Roediger**  
c/o Block Services  
Stuttgarter Str. 106  
70736 Fellbach  
Germany  

* **Email:** admin@42bit.email
* **Code Repository & Support:** [https://codeberg.org/nichu42/BoxViewer](https://codeberg.org/nichu42/BoxViewer)

If you have any questions or feedback regarding this policy, feel free to open an issue on the repository.

---

<a id="deutsche-version"></a>
# Deutsche Version: Impressum & Datenschutzerklärung

<a id="impressum"></a>
## Teil 1: Impressum (Anbieterkennzeichnung)

### Anbieter nach § 5 DDG:
**Nicolai Roediger**  
c/o Block Services  
Stuttgarter Str. 106  
70736 Fellbach  
Deutschland  

### Kontakt & Support:
* **E-Mail:** admin@42bit.email
* **Telefon:** +49 1579 2516692 (nur für dringende rechtliche Angelegenheiten)
* **Fax:** +49 6181 254505
* **Support & Fehlerberichte:** [https://codeberg.org/nichu42/BoxViewer/issues](https://codeberg.org/nichu42/BoxViewer/issues)

### Streitschlichtung:
Die Europäische Kommission stellt eine Plattform zur Online-Streitbeilegung (OSB) bereit: [https://ec.europa.eu/consumers/odr](https://ec.europa.eu/consumers/odr).  
Wir sind nicht bereit oder verpflichtet, an Streitbeilegungsverfahren vor einer Verbraucherschlichtungsstelle teilzunehmen.

### Haftungsausschluss:
Da es sich um ein freies, quelloffenes Softwareprojekt unter der [GPLv3-Lizenz](https://codeberg.org/nichu42/BoxViewer/src/branch/main/LICENSE) handelt, werden die Anwendung und diese Website "wie besehen" (ohne Mängelgewähr) und ohne jegliche ausdrückliche oder stillschweigende Gewährleistung bereitgestellt. Die Autoren oder Urheberrechtsinhaber sind in keinem Fall haftbar für Ansprüche, Schäden oder sonstige Haftung, die aus der Nutzung der Software entstehen.

---

<a id="datenschutz"></a>
## Teil 2: Datenschutzerklärung (DSGVO)

BoxViewer ist eine Open-Source-Companion-App für die openSenseMap-Plattform. Wir legen großen Wert auf Ihre Privatsphäre und verpflichten uns zu einer trackingfreien und sicheren Nutzung. Diese Datenschutzerklärung beschreibt, wie BoxViewer Daten verarbeitet.

### 1. Keine Erhebung oder Übermittlung personenbezogener Daten
BoxViewer erhebt, speichert oder übermittelt keine personenbezogenen Daten, Nutzungsstatistiken, Telemetriedaten, Absturzberichte oder Diagnosedaten an externe Server.
* Alle Fehlerprotokolle und Diagnoseberichte verbleiben **ausschließlich lokal** auf Ihrem Gerät.
* Es sind keine Telemetrie- oder Analyse-SDKs (wie Firebase, Google Analytics oder Crashlytics) integriert.

### 2. Netzwerkverbindungen
Zur Bereitstellung der Kernfunktionalitäten kommuniziert BoxViewer ausschließlich mit der offiziellen **openSenseMap-API**:
* **API-Host:** `https://api.opensensemap.org`
* **Zweck:** Abrufen öffentlicher senseBox-Konfigurationen, Metadaten und aktueller Sensormesswerte.
* Es werden keine sonstigen Server, Werbenetzwerke oder Tracking-Systeme kontaktiert.

### 3. Standortdaten
Wenn Sie BoxViewer die Freigabe des Standorts erlauben:
* Die App nutzt den standardmäßigen, nativen Android-`LocationManager`, um die Koordinaten Ihres Geräts zu bestimmen (über GPS, Mobilfunk oder passive Standortanbieter).
* **Lokale Verarbeitung:** Dieser Standort wird ausschließlich lokal auf Ihrem Gerät verwendet, um Entfernungen zu nahegelegenen senseBoxes zu berechnen oder Ihre Position auf der Umgebungskarte anzuzeigen.
* **Keine Weitergabe:** Ihre Standortkoordinaten werden niemals an unsere Server, an openSenseMap oder an sonstige Dritte übermittelt.

### 4. Lokaler SQLite-Speicher
Die App speichert Ihre Einstellungen, favorisierten senseBoxes und zwischengespeicherten Messwerte lokal in einer sicheren SQLite-Datenbank auf Ihrem Gerät.
* Diese Daten werden niemals mit Cloud-Diensten synchronisiert.
* Bei Deinstallation der App oder Löschen der App-Daten in den Android-Einstellungen werden alle gespeicherten Favoriten, Cache-Werte und Konfigurationen dauerhaft gelöscht.

### 5. Website-Hosting & Weiterleitungsdienste
Beim Besuch unserer Landingpage und Deep-Link-Weiterleitung (`https://nichu42.codeberg.page/BoxViewer`):
* **Server-Logfiles & Hosting:** Die Webseite wird auf Codeberg Pages gehostet (betrieben von Codeberg e.V., Berlin, Deutschland). Beim Laden der Seite übermittelt Ihr Webbrowser automatisch Verbindungsmetadaten (wie IP-Adresse, Browsertyp, Betriebssystem, Referrer-URL und Zugriffszeitstempel) an die Server von Codeberg. Gemäß der Datenschutzerklärung von Codeberg werden IP-Adressen bei der Protokollierung in Logfiles gekürzt, sodass sie nicht mit Ihren persönlichen Daten in Verbindung gebracht werden können, und maximal 7 Tage aufbewahrt. Diese Verarbeitung ist technisch notwendig, um die Webseiten sicher und stabil bereitzustellen (Art. 6 Abs. 1 lit. f DSGVO). Weitere Einzelheiten finden Sie in der [Datenschutzerklärung von Codeberg](https://codeberg.org/Codeberg/org/src/branch/main/PrivacyPolicy.md).
* **Kein Web-Tracking:** Die Website verwendet keine Cookies, lokale Tracking-Identifikatoren oder Drittanbieter-Webanalyse-Tools.
* **Lokales Weiterleitungsskript:** Das Skript extrahiert die senseBox-ID ausschließlich lokal in Ihrem Webbrowser, um die mobile Anwendung per Deep Link (`boxviewer://box/{id}`) zu starten. Es werden keine ID-Parameter oder Navigationsdaten an unsere Server übermittelt.

### 6. Kontakt & Datenschutzverantwortlicher
BoxViewer ist ein Open-Source-Projekt. Bei Fragen, Rückmeldungen oder datenschutzrechtlichen Anfragen können Sie sich an den Projektbetreiber und Verantwortlichen wenden:

**Nicolai Roediger**  
c/o Block Services  
Stuttgarter Str. 106  
70736 Fellbach  
Deutschland  

* **E-Mail:** admin@42bit.email
* **Code-Repository & Support:** [https://codeberg.org/nichu42/BoxViewer](https://codeberg.org/nichu42/BoxViewer)

Wenn Sie Fragen oder Anmerkungen zu dieser Datenschutzerklärung haben, können Sie gerne ein Ticket im Codeberg-Repository eröffnen.
