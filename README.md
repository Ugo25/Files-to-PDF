<p align="center">
  <img src="assets/banner.png" alt="Files to PDF banner" />
</p>

<h1 align="center">Files to PDF</h1>

<p align="center">
  Aplicación Java Swing para crear y editar PDFs: convierte imágenes y archivos de Office a PDF con vista previa y zoom, miniaturas, ordenar/rotar páginas, unir/dividir, marcas de agua e impresión.
</p>

<p align="center">
  <!-- Lenguaje principal -->
  <img alt="Java" src="https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white">
  <!-- Sistemas soportados -->
  <img alt="OS" src="https://img.shields.io/badge/SO-Windows%20%7C%20Linux%20%7C%20macOS-808080?logo=serverfault&logoColor=white">
  <!-- Build (requiere workflow maven.yml en GitHub Actions) -->
  <img alt="Build" src="https://img.shields.io/github/actions/workflow/status/<tu-usuario>/Files-to-PDF/maven.yml?label=Build&logo=apachemaven">
  <!-- Última release -->
  <img alt="Release" src="https://img.shields.io/github/v/release/<tu-usuario>/Files-to-PDF?color=blue&logo=github">
  <!-- Issues -->
  <img alt="Issues" src="https://img.shields.io/github/issues/<tu-usuario>/Files-to-PDF?logo=github">
  <!-- Lenguaje principal -->
  <img alt="Top language" src="https://img.shields.io/github/languages/top/<tu-usuario>/Files-to-PDF?logo=java&color=red">
  <!-- Repo size -->
  <img alt="Repo size" src="https://img.shields.io/github/repo-size/<tu-usuario>/Files-to-PDF?color=purple">
  <!-- Último commit -->
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/<tu-usuario>/Files-to-PDF?color=teal">
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

<p align="center">
  <img src="assets/screens/home.png" width="70%" alt="Pantalla de inicio">
</p>

<p align="center">
  <img src="assets/screens/preview.png" width="70%" alt="Vista previa del PDF">
</p>

<p align="center">
  <img src="assets/screens/cards.png" width="70%" alt="Acciones disponibles (cards)">
</p>

*(GIFs próximamente: demostración de arrastrar imágenes → exportar PDF → imprimir)*

---

## ✨ Características

- 🖼️ **Imágenes → PDF** (múltiples archivos, ordenar/rotar antes de exportar).  
- 🗂️ **Unir / Dividir PDF** por rango.  
- 🔤 **Extraer texto** a TXT.  
- 🏷️ **Marca de agua (texto)**.  
- 🖨️ **Imprimir** directamente.  
- 🔍 **Vista previa con zoom** fluido y miniaturas.  
- 📄 **Office → PDF** usando LibreOffice (opcional).  
- ⚙️ UI responsiva, caché de páginas y scroll suave.  
- 🎨 Tema claro/oscuro con persistencia.  
- 🗂️ Diálogos nativos para abrir/guardar archivos.  

---

## 📋 Requisitos

- **Java 17+** (JDK).  
- **LibreOffice** instalado (solo si usarás la conversión de Office → PDF).  
- **Maven** (si deseas compilar desde código fuente).  

---

## 🚀 Descargar / Ejecutar

### 🔹 Opción A: Ejecutar desde IDE
1. Clona el repo:
   ```bash
   git clone https://github.com/<tu-usuario>/Files-to-PDF.git
   cd Files-to-PDF
