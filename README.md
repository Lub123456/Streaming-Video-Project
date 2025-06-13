# -- Multimedia Streaming Project --

This project is a simple multimedia video streaming client-server application written in Java.\
It supports multiple streaming protocols, detects the client's bandwidth, and dynamically filters available video files.

---

## Features

- Java client-server architecture
- Bandwidth detection using real-time download test
- Adaptive video filtering based on network speed
- Support for **TCP**, **UDP**, and **RTP/UDP** protocols
- Client-side playback with `ffplay`
- Automatic video resolution conversion (using `FFMPEG`)
- Graphical user interface (Swing)

---

## Folder Structure

```
project-root/
│
├── videos/              # Video files and auto-generated variants
│   └── REQUIREMENTS.txt # Instructions for adding new videos
│
├── sdp/                 # SDP file generated for RTP streaming
│
├── src/
│   ├── client/          # Client GUI and logic
│   ├── server/          # Streaming server
│   ├── shared/          # Shared classes (VideoFile, Protocol)
│   └── utils/           # Logger, FFMPEG utilities
```

---

## How to Run

### Server

1. Place your videos in the `videos/` folder (see [REQUIREMENTS](videos/REQUIREMENTS.txt))
2. Launch the server:

```bash
java server.StreamingServer
```


### Client

1. Launch the GUI client:

```bash
java client.ClientGUI
```

2. Select a video format (e.g. `mp4`)
3. The app will automatically test your download speed
4. Choose a video and a protocol (TCP, UDP, RTP/UDP or Auto)
5. Click **Play Video**\
   → The server starts streaming, and the video opens via `ffplay`

---

## ⚙Requirements

- Java 17+ (or compatible)
- `FFMPEG` + `FFPLAY` installed and accessible in PATH
- Internet access for bandwidth test
- Ports:
  - Server/Client communication: `9090`
  - TCP: `8888`
  - UDP: `8888`
  - RTP: `5004` (video), `5006` (audio)

---

## Video Naming Rules

All video files **must** follow this naming format:

```
<name>-<resolution>.<format>
```

Examples:

```
myfilm-240p.mp4
MovieName-720p.avi
Test_Movie-1080p.mkv
```

Accepted formats: `mp4`, `avi`, `mkv`

> Files not matching this format will be ignored.

---

## Notes

- The server auto-generates lower resolution versions using `FFMPEG`
- Playback automatically closes the client GUI when finished
- The system filters available videos based on your connection speed using recommended bitrates:
  - 240p → 0.4 Mbps
  - 360p → 0.75 Mbps
  - 480p → 1.0 Mbps
  - 720p → 2.5 Mbps
  - 1080p → 4.5 Mbps

---

## Author

Lubin TERRIEN – 2025\
Student project for Multimedia & Multimedia Communications (Java) class at University of West Attica (Athens, GREECE).
