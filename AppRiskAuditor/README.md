# NCASM_Hackathon-Cyber_Blaze

# 🛡️ On-Device App Risk Auditor

### An Android Security Tool for Permission-Based Risk Analysis

## 📌 Project Overview

The On-Device App Risk Auditor is an Android security project designed to help users identify potentially risky applications installed on their devices. It analyzes app permissions, special access settings, and combinations of sensitive capabilities to generate an understandable risk score.

The project aims to improve user awareness by explaining *why* an application may be risky and helping users review its permissions through Android system settings.

## ❗ Problem Statement

Android applications may request access to sensitive device features such as SMS, contacts, location, camera, microphone, and accessibility services. Users may not always understand the risks associated with these permissions or combinations of capabilities.

There is a need for a simple, privacy-focused tool that analyzes installed applications, highlights suspicious permission patterns, and provides clear explanations so users can make informed security decisions.

## 💡 Proposed Solution

We propose an Android application that scans installed apps and evaluates their declared permissions, granted permissions, and available special-access indicators.

The auditor will use a rule-based risk assessment system to assign risk scores and classify applications into risk levels. It will also provide plain-language explanations for flagged applications and shortcuts to relevant Android system settings.

## 🎯 Objectives

- Identify sensitive permissions and special access associated with installed applications.
- Categorize app capabilities into meaningful security groups.
- Calculate an understandable risk score for each analyzed application.
- Explain the reasons behind high-risk warnings.
- Help users review app settings and take appropriate action.
- Prioritize on-device processing and user privacy.

## ✨ Planned Features

- *App Inventory:* Display installed application information, including app name, package name, version, and other available metadata.
- *Permission Analysis:* Examine declared and granted permissions where Android permits access.
- *Special Access Detection:* Identify supported indicators such as accessibility services, device administrators, notification              listeners, overlay access, and package-installation access.
- *Risk Scoring:* Assign a score from 0–100 using configurable, rule-based criteria.
- *Risk Classification:* Group applications into Low, Medium, High, and Critical risk levels.
- *Explainable Warnings:* Show human-readable reasons for applications classified as high risk.
- *App Management Shortcuts:* Help users open Android system settings to review an application's access and permissions.
- *Privacy-Focused Design:* Perform core analysis locally on the device wherever possible.

## 🔍 Permission Categories

The planned analysis covers nine categories:

1. *Special Access* – Sensitive privileges such as accessibility and overlay access.
2. *SMS* – Access to text messages and related operations.
3. *Contacts & Call Logs* – Access to contacts and call history.
4. *Camera & Microphone* – Access to camera and audio recording.
5. *Location* – Access to approximate or precise location.
6. *Phone & Identity* – Access to supported phone-related information.
7. *Storage & Media* – Access to files, photos, and media.
8. *Background & Persistence* – Indicators of background activity or persistence-related capabilities.
9. *Network* – Network-related permissions and relevant combinations with other capabilities.

## ⚙️ How It Works

1. *Scan:* Collect information about installed applications that Android makes available.
2. *Analyze:* Examine permissions, special-access indicators, and relevant combinations.
3. *Evaluate:* Apply predefined rules to calculate a risk score.
4. *Explain:* Display the risk level and reasons behind a warning.
5. *Review:* Help the user open the relevant system settings to inspect the application.

## 🔐 Privacy & Security

- Core analysis is intended to run on the device.
- The project follows an offline-first approach wherever practical.
- Personal app information is not intended to be uploaded to a server for routine analysis.
- Any optional external threat-intelligence lookup should require explicit user consent.
- The tool is designed to help users understand application risks without requiring root access.

## 🧰 Technology Stack

*Update this section to match the technologies actually selected and used by the development team.*

- *Platform:* Android
- *Programming Language:* To be finalized by the team
- *Development Environment:* To be finalized by the team
- *UI Framework:* To be finalized by the team
- *Risk Analysis:* Rule-based permission analysis
- *Version Control:* Git and GitHub


Planned development milestones include:

## 🚧 Planned Development Milestones

- [ ] *Android App Scanner:* Retrieve installed app details, including name, icon, package name, version, installation date, installer source, target SDK, and requested permissions with their granted status.
- [ ] *Permission Categorization:* Organize app permissions into nine categories: Special Access, SMS, Contacts & Call Logs, Camera & Microphone, Location, Phone & Identity, Storage & Media, Background & Persistence, and Network.
- [ ] *Special Access Detection:* Identify supported indicators for Accessibility Services, Device Administrators, Notification Listeners, Overlay Access, Install-App Access, and Usage Stats.
- [ ] *Risk Scoring Engine:* Implement a rule-based scoring system that assigns each app a risk score from 0–100 and classifies it as Low, Medium, High, or Critical.
- [ ] *Suspicious Pattern Detection:* Implement permission-combination rules and context-mismatch checks to identify potentially concerning app configurations.

*Milestones will be marked complete as the corresponding features are implemented and tested.*

## 🔮 Future Enhancements

- APK SHA-256 hash lookup against an external threat-intelligence source, with user consent.
- Static APK manifest analysis.
- Optional on-device machine-learning-based risk analysis.
- Monitoring supported app installation or update changes.
- Exportable PDF risk reports.
- Improved detection of suspicious combinations of permissions and special access.

## 👥 Team

Team members :

| Team Member | 
| ----------- | 
| Sagun Singh | 
| Shweta | 
| Sanjeevani | 
| Harnish |
| Ankit |
| Swastik |

## ⚠️ Limitations

- Android restricts access to certain installed-app information and special-access states.
- Available information may differ across Android versions and device manufacturers.
- Permission-based analysis cannot conclusively determine whether an app is malicious.
- Risk scores depend on the quality and coverage of the implemented rules.

## 📄 Disclaimer

This project is intended for educational and defensive security purposes. It helps users review application permissions and identify potentially risky configurations. It is not a guarantee of malware detection or a replacement for a dedicated mobile security solution.

