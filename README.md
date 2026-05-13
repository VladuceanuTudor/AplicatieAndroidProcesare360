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
├── SetupUrlActivity          — Prima lansare: introducere URL server backend
├── LoginActivity             — Autentificare username + parolă + indicator loading
├── MainActivity              — Dashboard cu statistici live + navigare + buton Logout
├── LibraryActivity           — Bibliotecă media (ListView / GridView)
│   └── DetailActivity        — Detalii panoramă + RatingBar + VR toggle + buton pipeline
│       └── VRViewerActivity  — Viewer VR (imagine sau video 360°)
├── UploadActivity            — Încărcare fișier local cu coordonate GPS + opțiuni procesare
├── MapActivity               — Google Maps cu markeri colorați + Polyline
├── StatsActivity             — Grafice MPAndroidChart
└── SettingsActivity          — SharedPreferences: afișare, mod VR (IPD), cache, filtru dată
```

---

## Structura proiectului

```
app/src/main/
├── java/com/example/aplicatieandroidprocesare360/
│   ├── SetupUrlActivity.java              — Ecran configurare URL server (prima lansare)
│   ├── LoginActivity.java                 — Ecran autentificare cu ProgressBar
│   ├── MainActivity.java                  — Dashboard + routing auth + meniu logout
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
│   │   ├── ApiClient.java             — Singleton Retrofit + OkHttp interceptor JWT + clearToken
│   │   ├── ProcessingService.java     — Interfață Retrofit pentru pipeline
│   │   └── model/
│   │       ├── LoginRequest.java
│   │       ├── LoginResponse.java
│   │       ├── JobCreateResponse.java
│   │       └── ProcessingJob.java
│   └── vr/
│       ├── SphericalRenderer.java     — GL renderer pentru imagini panoramice
│       └── VideoSphericalRenderer.java— GL renderer pentru video 360°
└── res/
    ├── layout/                        — 12 fișiere XML layout (MaterialCardView-based)
    ├── values/                        — strings, colors, themes, dimens
    ├── drawable/
    │   ├── bg_header_gradient.xml     — Gradient 150°: #1C2680 → #09090F
    │   ├── shape_input_field.xml      — Fundal EditText rotunjit (8dp)
    │   └── ic_back.xml
    └── menu/
        ├── menu_library.xml
        └── menu_main.xml              — Meniu toolbar MainActivity (Schimbă server, Deconectare)
```

---

## Schema bazei de date SQLite

### Tabel `panoramas`
| Coloană | Tip | Descriere |
|---|---|---|
| id | INTEGER PK | Identificator unic |
| title | TEXT | Titlul panoramei |
| file_path | TEXT | Cale fișier local sau URI content:// |
| thumbnail_url | TEXT | URL thumbnail |
| upload_date | INTEGER | Timestamp Unix (ms) |
| latitude | REAL | Coordonată GPS |
| longitude | REAL | Coordonată GPS |
| status | TEXT | PENDING / UPLOADING / PROCESSING / DONE / FAILED |
| job_id | TEXT | ID job returnat de pipeline |
| result_url | TEXT | URL stream rezultat procesat |
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

Backend-ul este un server **FastAPI** la `http://<server>/api/`. Toate endpoint-urile necesită header `Authorization: Bearer <token>` (obținut la login), cu excepția `stream` care acceptă și `?token=<jwt>` ca query param.

```
POST auth/login
  Body: { "username": "...", "password": "..." }
  Response: { "access_token": "...", "token_type": "bearer" }

POST jobs
  Body: multipart/form-data — câmp "video" (fișierul imagine/video)
  Response: { "job_id": "abc123", "status": "queued" }

GET jobs/{job_id}/status
  Response: {
    "id": "abc123",
    "status": "done | processing | failed",
    "progress_pct": 75.0,
    "error": null
  }

GET jobs/{job_id}/stream
  Auth: header Authorization: Bearer <token>  SAU  ?token=<jwt>
  Response: stream video (folosit de ExoPlayer)
```

URL-ul stream este construit în aplicație la finalizarea procesării:
```
http://<server>/api/jobs/<job_id>/stream?token=<jwt>
```

---

## Configurare și instalare

### 1. Cheie Google Maps
Adaugă în fișierul `local.properties` (ignorat de Git):
```
MAPS_API_KEY=cheia_ta_aici
```
Obții cheia din [Google Cloud Console](https://console.cloud.google.com/) cu **Maps SDK for Android** activat.

### 2. Configurare server în aplicație
- Pornește pipeline-ul (Docker stack sau direct)
- La prima lansare, aplicația afișează automat ecranul **Configurare server**
- Introdu IP-ul serverului (ex: `192.168.1.x`) — aplicația adaugă automat `/api/`
- Introdu username și parola pe ecranul de **Autentificare** → token JWT salvat automat
- Din dashboard, butonul **Deconectare** revine la ecranul de login
- Din meniul ⋮ al dashboard-ului, **Schimbă server** revine la ecranul de URL

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
- Controale: play/pause, seekbar, timp curent/total; video se repetă automat

### Distanța interpupilară (IPD)
Configurabilă în **Setări** prin slider (50–90 mm, default 65 mm).

---

## Design system

Interfața folosește un sistem de design dark, inspirat din Material You.

| Token | Valoare |
|---|---|
| `bg_dark` | `#09090F` |
| `surface_dark` | `#111119` |
| `card_dark` | `#181826` |
| `border` | `#252540` |
| `primary` | `#4D8EFF` |
| `accent` | `#00E5FF` |
| `status_done` | `#00E676` |
| `status_failed` | `#FF5252` |

Toate ecranele folosesc `MaterialCardView` cu rază 14dp și bordură 1dp. Badge-urile de status sunt colorate programatic prin `GradientDrawable`.

---

## Cerințe temă bifate

| Nr. | Cerință | Implementare |
|---|---|---|
| 1 | Minim 5 activități | 10 activități legate între ele |
| 2 | Controale vizuale simple | TextView, EditText, Spinner, Button, CheckBox, ProgressBar, RatingBar, Switch |
| 3 | Controale vizuale complexe | ListView, GridView, DatePicker |
| 4 | Custom adapter ListView | `PanoramaListAdapter` și `PanoramaGridAdapter` (BaseAdapter) |
| 5 | SharedPreferences | URL API, token JWT, username, calitate implicită, mod afișare, IPD, dată filtru |
| 6 | SQLite | Tabele `panoramas` + `processing_log` |
| 7 | Parsare JSON de la distanță | Pipeline API (Retrofit + Gson): login, upload, status |
| 8 | Google Maps + poligoane | Markeri colorați după status + Polyline între panorame |
| 9 | Grafică 2D | 4 grafice MPAndroidChart (BarChart, PieChart, LineChart) |
| + | **Extra: VR Cardboard** | OpenGL ES 2.0 sferă + giroscop + split-screen |
| + | **Extra: Video 360°** | ExoPlayer + GL_TEXTURE_EXTERNAL_OES + shader extern |
| + | **Extra: Autentificare JWT** | Login Bearer token + OkHttp interceptor + persistență |

---

## Autor

Tudor Vlăduceanu — Proiect Android, conectat cu tema de licență în Computer Vision / Procesare imagini panoramice 360°.
