# PanoramaVR 360

Aplicație Android pentru încărcarea, procesarea și vizualizarea imaginilor panoramice 360° și a videoclipurilor 360°, conectată la un pipeline de procesare prin computer vision (tema de licență).

---

## Descriere

PanoramaVR 360 permite utilizatorului să:
- încarce imagini panoramice (echirectangulare) sau videoclipuri 360° dintr-un fișier local
- importe imagini panoramice direct din **Mapillary** (API gratuit) pe baza coordonatelor GPS
- previzualizeze locații prin **Google Street View Static API**
- trimită conținutul la un pipeline de procesare prin computer vision (rulat local sau remote)
- vizualizeze rezultatele în **mod normal** sau în **mod VR Cardboard** (split-screen cu giroscop)
- urmărească statistici de procesare prin grafice interactive

---

## Cerințe sistem

| Cerință | Valoare |
|---|---|
| Android minim | API 26 (Android 8.0 Oreo) |
| Android țintă | API 36 |
| OpenGL ES | 2.0 |
| Permisiuni | INTERNET, ACCESS_FINE_LOCATION, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO |

---

## Tehnologii utilizate

| Categorie | Bibliotecă / API |
|---|---|
| HTTP / REST | Retrofit 2.9 + OkHttp 4.12 |
| Parsare JSON | Gson |
| Încărcare imagini | Glide 4.16 |
| Grafice 2D | MPAndroidChart v3.1 |
| Hărți | Google Maps SDK 19.0 |
| Localizare GPS | Google Play Services Location 21.3 |
| Redare video | ExoPlayer (Media3) 1.3.1 |
| Baze de date | SQLite (SQLiteOpenHelper) |
| Preferințe | SharedPreferences |
| VR renderer | OpenGL ES 2.0 (implementare custom) |

---

## Arhitectura aplicației

```
PanoramaVR 360
├── MainActivity              — Dashboard cu statistici live
├── LibraryActivity           — Bibliotecă media (ListView / GridView)
│   └── DetailActivity        — Detalii panoramă + RatingBar + VR toggle
│       └── VRViewerActivity  — Viewer VR (imagine sau video 360°)
├── UploadActivity            — Încărcare fișier local sau import Mapillary
├── MapActivity               — Google Maps cu markeri + Street View preview
├── StatsActivity             — Grafice MPAndroidChart
└── SettingsActivity          — SharedPreferences + CalendarView + SeekBar IPD
```

---

## Structura proiectului

```
app/src/main/
├── java/com/example/aplicatieandroidprocesare360/
│   ├── MainActivity.java
│   ├── LibraryActivity.java
│   ├── DetailActivity.java
│   ├── UploadActivity.java
│   ├── MapActivity.java
│   ├── StatsActivity.java
│   ├── SettingsActivity.java
│   ├── VRViewerActivity.java
│   ├── model/
│   │   └── Panorama.java              — Model de date principal
│   ├── database/
│   │   └── DatabaseHelper.java        — SQLite: tabele panoramas + processing_log
│   ├── adapter/
│   │   ├── PanoramaListAdapter.java   — Custom BaseAdapter pentru ListView
│   │   └── PanoramaGridAdapter.java   — Custom BaseAdapter pentru GridView
│   ├── api/
│   │   ├── ApiClient.java             — Singleton Retrofit clients
│   │   ├── MapillaryService.java      — Interfață Retrofit pentru Mapillary v4
│   │   ├── ProcessingService.java     — Interfață Retrofit pentru pipeline
│   │   └── model/
│   │       ├── MapillaryImage.java
│   │       └── ProcessingJob.java
│   └── vr/
│       ├── SphericalRenderer.java     — GL renderer pentru imagini panoramice
│       └── VideoSphericalRenderer.java— GL renderer pentru video 360°
└── res/
    ├── layout/                        — 10 fișiere XML layout
    ├── values/                        — strings, colors, themes, dimens
    ├── menu/
    │   └── menu_library.xml
    └── drawable/
        └── ic_back.xml
```

---

## Schema bazei de date SQLite

### Tabel `panoramas`
| Coloană | Tip | Descriere |
|---|---|---|
| id | INTEGER PK | Identificator unic |
| title | TEXT | Titlul panoramei |
| file_path | TEXT | Cale fișier local sau URL |
| thumbnail_url | TEXT | URL thumbnail (Mapillary / rezultat) |
| upload_date | INTEGER | Timestamp Unix (ms) |
| latitude | REAL | Coordonată GPS |
| longitude | REAL | Coordonată GPS |
| status | TEXT | PENDING / UPLOADING / PROCESSING / DONE / FAILED |
| job_id | TEXT | ID job returnat de pipeline |
| result_url | TEXT | URL rezultat procesat |
| depth_map_url | TEXT | URL depth map |
| quality_score | REAL | Scor calitate 0.0–1.0 |
| processing_time_ms | INTEGER | Durata procesării |
| rating | REAL | Rating utilizator 0–5 |
| source_type | TEXT | LOCAL / MAPILLARY / STREETVIEW |

### Tabel `processing_log`
| Coloană | Tip | Descriere |
|---|---|---|
| id | INTEGER PK | |
| panorama_id | INTEGER | FK → panoramas.id |
| timestamp | INTEGER | Timestamp Unix |
| step_name | TEXT | Numele pasului de procesare |
| duration_ms | INTEGER | Durata pasului |
| success | INTEGER | 0 / 1 |

---

## API-uri externe

### Pipeline de procesare (propriu)
Aplicația se conectează la backend-ul tău prin aceste endpoint-uri:

```
POST /upload
  Body: multipart/form-data
    - file: fișierul imagine/video
    - quality: low | medium | high | ultra
    - depth_estimation: true | false
    - mesh_generation: true | false
    - color_correction: true | false
    - hdr: true | false
  Response: { "job_id": "abc123", "status": "pending" }

GET /status/{job_id}
  Response: {
    "job_id": "abc123",
    "status": "done | processing | failed",
    "progress": 75,
    "processing_time_ms": 4200,
    "quality_score": 0.87,
    "error_message": null
  }

GET /result/{job_id}
  Response: {
    "result_url": "http://...",
    "depth_map_url": "http://...",
    "processing_time_ms": 4200,
    "quality_score": 0.87
  }
```

### Mapillary v4 API (gratuit)
```
GET https://graph.mapillary.com/images
  ?fields=id,geometry,thumb_2048_url,thumb_original_url
  &bbox={lng1},{lat1},{lng2},{lat2}
  &limit=20
  &access_token={token}
```

### Google Street View Static API
```
GET https://maps.googleapis.com/maps/api/streetview
  ?size=640x360
  &location={lat},{lng}
  &fov=90
  &heading=0
  &key={MAPS_API_KEY}
```

---

## Configurare și instalare

### 1. Cheie Google Maps
În `app/build.gradle.kts`, înlocuiește:
```kotlin
manifestPlaceholders["MAPS_API_KEY"] = "YOUR_MAPS_API_KEY_HERE"
```
cu cheia ta obținută din [Google Cloud Console](https://console.cloud.google.com/).

Servicii necesare activate: **Maps SDK for Android**, **Street View Static API**.

### 2. Token Mapillary
- Creează cont pe [mapillary.com](https://www.mapillary.com/)
- Mergi la Settings → Developers → Create Application
- Copiază **Client Access Token**
- Introdu-l în aplicație: **Setări → Token Mapillary**

### 3. URL pipeline
- Pornește pipeline-ul tău local
- Dacă e nevoie de acces de pe telefon: rulează `ngrok http 5000`
- Introdu URL-ul în aplicație: **Setări → URL endpoint procesare**

### 4. Sincronizare Gradle
```
File → Sync Project with Gradle Files
```

---

## Vizualizare VR (mod Cardboard)

Viewer-ul VR este implementat nativ în **OpenGL ES 2.0**, fără SDK extern.

### Imagini panoramice
- Imaginea echirectangulară este mapată pe interiorul unei sfere 3D (32×64 segmente)
- Rotația camerei este controlată de senzorul `TYPE_ROTATION_VECTOR` (giroscop + accelerometru + magnetometru)
- În modul Cardboard, ecranul este împărțit în două viewport-uri cu offset IPD configurabil

### Video 360°
- Video-ul este decodat de **ExoPlayer** și trimis pe un `SurfaceTexture`
- `SurfaceTexture` folosește textura `GL_TEXTURE_EXTERNAL_OES` (extensie hardware pentru video)
- Shader-ul fragment aplică matricea de transformare a `SurfaceTexture` pentru orientare corectă
- Controale: play/pause, seekbar, timp curent/total
- Video-ul se repetă automat (loop)

### Distanța interpupilară (IPD)
Configurabilă în **Setări** prin slider (50–90 mm, default 65 mm).
Valoarea IPD este transmisă renderer-ului și aplicată ca offset orizontal între cele două viewport-uri.

---

## Cerințe temă bifate

| Nr. | Cerință | Implementare |
|---|---|---|
| 1 | Minim 5 activități | 8 activități legate între ele |
| 2 | Controale vizuale simple | TextView, EditText, Spinner, Button, CheckBox, ProgressBar, RatingBar, Switch |
| 3 | Controale vizuale complexe | ListView, GridView, CalendarView, DatePicker (SeekBar IPD) |
| 4 | Custom adapter ListView | `PanoramaListAdapter` și `PanoramaGridAdapter` (BaseAdapter) |
| 5 | SharedPreferences | URL API, token Mapillary, calitate, IPD, mod afișare |
| 6 | SQLite | Tabele `panoramas` + `processing_log` |
| 7 | Parsare JSON de la distanță | Mapillary API + pipeline API (Retrofit + Gson) |
| 8 | Google Maps + poligoane | Markeri colorați pe status + Polyline între panorame |
| 9 | Grafică 2D | 4 grafice MPAndroidChart (BarChart, PieChart, LineChart) |
| + | **Extra: VR Cardboard** | OpenGL ES 2.0 sferă + giroscop + split-screen |
| + | **Extra: Video 360°** | ExoPlayer + GL_TEXTURE_EXTERNAL_OES + shader extern |
| + | **Extra: Street View** | Preview Street View Static API pe hartă |
| + | **Extra: Mapillary** | Import panorame reale echirectangulare |

---

## Autor

Tudor Vlăduceanu — Proiect Android, conectat cu tema de licență în Computer Vision / Procesare imagini panoramice 360°.
