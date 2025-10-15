<h1 align="center">🧩 Files to PDF — v1.0.0</h1>

<p align="center">
  Aplicación Java Swing moderna para **crear, unir, dividir y editar archivos PDF**, con interfaz visual basada en <b>FlatLaf</b>, vista previa integrada y herramientas de conversión desde imágenes y Office.
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white">
  <img alt="OS" src="https://img.shields.io/badge/SO-Windows%20%7C%20Linux%20%7C%20macOS-808080?logo=serverfault&logoColor=white">
  <img alt="Build" src="https://img.shields.io/github/actions/workflow/status/Ugo25/Files-to-PDF/maven.yml?branch=main&label=Build&logo=apachemaven">
  <img alt="Release" src="https://img.shields.io/github/v/release/Ugo25/Files-to-PDF?color=blue&logo=github">
  <img alt="Issues" src="https://img.shields.io/github/issues/Ugo25/Files-to-PDF?logo=github">
  <img alt="Top language" src="https://img.shields.io/github/languages/top/Ugo25/Files-to-PDF?logo=java&color=red">
  <img alt="Repo size" src="https://img.shields.io/github/repo-size/Ugo25/Files-to-PDF?color=purple">
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/Ugo25/Files-to-PDF?color=teal">
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue"></a>
</p>

---

## 📖 Tabla de contenido
- [📸 Capturas](#-capturas)
- [✨ Características](#-características)
- [📋 Requisitos](#-requisitos)
- [🚀 Descargar / Ejecutar](#-descargar--ejecutar)
- [📂 Estructura del proyecto](#-estructura-del-proyecto)
- [🧰 Roadmap](#-roadmap)
- [🔒 Privacidad](#-privacidad)
- [💡 Inspiración](#-inspiración)
- [👥 Créditos](#-créditos)
- [🤝 Contribuir](#-contribuir)
- [📜 Licencia](#-licencia)
- [⭐ Apóyame](#-apóyame)

---

## 📸 Capturas

| Inicio | Menú principal |
|---|---|
| ![Inicio](screenshots/inicio.png) | ![Menú](screenshots/opciones.png) |

*(GIFs próximamente: demostración de arrastrar imágenes → exportar PDF → imprimir)*

---

## ✨ Características

- 🖼️ **Imágenes → PDF** (múltiples archivos, ordenar/rotar antes de exportar).  
- 🗂️ **Unir / Dividir PDF** por rango.  
- 🔤 **Extraer texto** a TXT.  
- 🏷️ **Marca de agua (texto o imagen)**.  
- 🖨️ **Impresión directa** desde la vista previa.  
- 🔍 **Zoom y miniaturas** para explorar el documento.  
- 📄 **Office → PDF** mediante LibreOffice (opcional).  
- 🎨 **Tema claro/oscuro persistente** entre sesiones.  
- ⚙️ **Interfaz responsiva** con scroll fluido y diálogos nativos.  

---

## 📋 Requisitos

- ☕ **Java 17+ (JDK)**  
- 🧩 **Maven** (para compilar desde código fuente)  
- 📝 **LibreOffice** (solo si usarás la conversión Office → PDF)  

---

## 🚀 Descargar / Ejecutar

### 🔹Ejecutar desde IDE

```bash
git clone https://github.com/Ugo25/Files-to-PDF.git
cd Files-to-PDF
mvn clean install
java -jar target/Files-to-PDF-1.0.0.jar
```

---

## 📂 Estructura del proyecto
```plaintext
src/
 ├── main/java/
 │    └── com/mycompany/programa_pdf/
 │         ├── MainWindow.java
 │         ├── PdfPreviewDialog.java
 │         ├── MergePreviewDialog.java
 │         ├── WatermarkPreviewDialog.java
 │         └── ...
 └── resources/
      ├── icons/
      ├── themes/
      └── config/
```
---

## Roadmap

 Unir / dividir PDFs
 
 Marcas de agua
 
 PDF → imágenes

 Office → PDF

 OCR (reconocimiento de texto en imágenes)

 Firmas digitales

 Exportar a PDF interactivo con metadatos

---
## 🔒 Privacidad

Toda la conversión y edición se realiza localmente en tu dispositivo.
No se almacenan ni envían archivos a ningún servidor externo.

---

## Inspiración

El diseño está inspirado en la simplicidad de PDFsam y la interfaz moderna de LibreOffice Draw, con un enfoque visual propio.

---

## Créditos

Desarrollado por Hugo Acosta (Ugo25)
📍 Mazatlán, Sinaloa — Bytemar Tech
🗓️ Versión: 1.0.0 (2025)

```bash
Files to PDF 1.0.0 (2025)
```
---

## Contribuir

Haz un fork del repositorio
Crea una rama con tu cambio:
```bash
git checkout -b feature/nueva-funcion
```

---
## Licencia

Este proyecto está bajo la licencia Apache 2.0.

---

## Apóyame

Si te fue útil, deja una ⭐ en el repositorio y comparte Files to PDF
