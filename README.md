<p align="center">
  <img src="screenshots/banner.png" width="60%" alt="Files to PDF banner" />
</p>

<h1 align="center">🧩 Files to PDF — v1.0.0</h1>

<p align="center">
  Aplicación Java Swing moderna para **crear, unir, dividir y editar archivos PDF**, con interfaz visual basada en <b>FlatLaf</b>, vista previa integrada y herramientas de conversión desde imágenes y Office.
</p>

<p align="center">
  <!-- Lenguaje principal -->
  <img alt="Java" src="https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white">
  <!-- Sistemas soportados -->
  <img alt="OS" src="https://img.shields.io/badge/SO-Windows%20%7C%20Linux%20%7C%20macOS-808080?logo=serverfault&logoColor=white">
  <!-- Build -->
  <img alt="Build" src="https://img.shields.io/github/actions/workflow/status/Hugo-Acosta/Files-to-PDF/maven.yml?label=Build&logo=apachemaven">
  <!-- Última release -->
  <img alt="Release" src="https://img.shields.io/github/v/release/Hugo-Acosta/Files-to-PDF?color=blue&logo=github">
  <!-- Issues -->
  <img alt="Issues" src="https://img.shields.io/github/issues/Hugo-Acosta/Files-to-PDF?logo=github">
  <!-- Lenguaje principal -->
  <img alt="Top language" src="https://img.shields.io/github/languages/top/Hugo-Acosta/Files-to-PDF?logo=java&color=red">
  <!-- Repo size -->
  <img alt="Repo size" src="https://img.shields.io/github/repo-size/Hugo-Acosta/Files-to-PDF?color=purple">
  <!-- Último commit -->
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/Hugo-Acosta/Files-to-PDF?color=teal">
  <!-- License -->
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

| Inicio | Menú principal | Insertar imagen |
|---|---|---|
| ![Inicio](assets/screens/home.png) | ![Menú](assets/screens/preview.png) | ![Insertar](assets/screens/cards.png) |

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

### 🔹 Opción A: Ejecutar desde IDE

```bash
git clone https://github.com/Hugo-Acosta/Files-to-PDF.git
cd Files-to-PDF
mvn clean install
java -jar target/Files-to-PDF-1.0.0.jar
