# PanoramaVR 360

Aplicație Android pentru încărcarea, procesarea și vizualizarea imaginilor panoramice 360° și a videoclipurilor 360°, conectată la un pipeline de procesare prin computer vision (tema de licență).

---

## Descriere

PanoramaVR 360 permite utilizatorului să:
- încarce imagini panoramice (echirectangulare) sau videoclipuri 360° dintr-un fișier local
- atașeze coordonate GPS automat (prin locația dispozitivului) sau manual
- trimită conținutul la un pipeline de procesare prin computer vision (rulat local sau remote)
- vizualizeze panoramele pe hartă, filtrate după status
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
├── UploadActivity            — Încărcare fișier local cu coordonate GPS
├── MapActivity               — Google Maps cu markeri colorați + Polyline
├── StatsActivity             — Grafice MPAndroidChart
└── SettingsActivity          — SharedPreferences + CalendarView + DatePicker + SeekBar IPD
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
│   │   ├── ApiClient.java             — Singleton Retrofit client
│   │   ├── ProcessingService.java     — Interfață Retrofit pentru pipeline
│   │   └── model/
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
| file_path | TEXT | Cale fișier local |
| thumbnail_url | TEXT | URL thumbnail (rezultat procesat) |
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
| source_type | TEXT | LOCAL |

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

## API pipeline de procesare

Aplicația se conectează la backend-ul tău prin aceste endpoint-uri REST care returnează JSON:

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

---

## Configurare și instalare

### 1. Cheie Google Maps
Adaugă în fișierul `local.properties` (creat automat de Android Studio, ignorat de Git):
```
MAPS_API_KEY=cheia_ta_aici
```
Obții cheia din [Google Cloud Console](https://console.cloud.google.com/) cu serviciul **Maps SDK for Android** activat.

### 2. URL pipeline
- Pornește pipeline-ul tău local
- Dacă e nevoie de acces de pe telefon în rețea locală: folosește IP-ul mașinii (ex: `http://192.168.1.x:5000`)
- Sau rulează `ngrok http 5000` pentru acces extern
- Introdu URL-ul în aplicație: **Setări → URL endpoint procesare**

### 3. Sincronizare Gradle
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
| 3 | Controale vizuale complexe | ListView, GridView, CalendarView, DatePicker |
| 4 | Custom adapter ListView | `PanoramaListAdapter` și `PanoramaGridAdapter` (BaseAdapter) |
| 5 | SharedPreferences | URL API, calitate implicită, mod afișare, IPD, dată filtru |
| 6 | SQLite | Tabele `panoramas` + `processing_log` |
| 7 | Parsare JSON de la distanță | Pipeline API (Retrofit + Gson): upload, status, result |
| 8 | Google Maps + poligoane | Markeri colorați după status + Polyline între panorame |
| 9 | Grafică 2D | 4 grafice MPAndroidChart (BarChart, PieChart, LineChart) |
| + | **Extra: VR Cardboard** | OpenGL ES 2.0 sferă + giroscop + split-screen |
| + | **Extra: Video 360°** | ExoPlayer + GL_TEXTURE_EXTERNAL_OES + shader extern |

---

## Autor

Tudor Vlăduceanu — Proiect Android, conectat cu tema de licență în Computer Vision / Procesare imagini panoramice 360°.
