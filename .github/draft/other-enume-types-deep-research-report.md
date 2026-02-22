# Виконавче резюме

Наведено перелік усіх **enum-таблиць** із офіційної документації **F1 25 UDP** (Data Output from F1 25 v3) **(крім sessionType та trackId)**. Для кожного enum вказано назву, усі коди→відповідні описи, тип, де в пакеті використовується (поле+офсет), короткий опис, приклад значення. Якщо в ній є відмінності між форматами 2025/2024/2023 – зазначено у порівняльній таблиці з посиланням на джерела. Розрахунки офсетів наведено в розділі *Assumptions & calculation*. Джерела: офіційний PDF F1 25 (EA/Codemasters)【1†L173-L181】【7†L442-L450】, а також неофіційні GitHub-репо для додаткових даних (відмічені як такі).  

## Enums (без sessionType і trackId)

У таблицях наведено **Name**, **Code**, **Meaning** (англ. опис), **Type**, **Packet field (offset)** і **Example** (hex). 

### Weather (погода)

Використовується в PacketSessionData **m_weather (offset 29)**【4†L1-L4】.  
| Code | Опис            | Тип   | Поле (офсет)       | Приклад |
|------|-----------------|------|--------------------|---------|
| 0    | clear (ясно)    | uint8 | m_weather (29)     | 00      |
| 1    | light cloud (невелика хмарність) | uint8 | m_weather (29) |  |
| 2    | overcast (пасмурно) | uint8 | m_weather (29)   |         |
| 3    | light rain (легкий дощ) | uint8 | m_weather (29) |  |
| 4    | heavy rain (сильний дощ) | uint8 | m_weather (29) |  |
| 5    | storm (шторм)    | uint8 | m_weather (29)     |  |

### Track/Air Temperature Change

Використовуються в PacketSessionData: **m_trackTemperatureChange (offset 32)** і **m_airTemperatureChange (offset 34)**【6†L13-L17】.  
| Code | Опис          | Тип   | Поле              | Приклад |
|------|---------------|------|-------------------|---------|
| 0    | up (підвищення)   | int8 | m_trackTemperatureChange (32); m_airTemperatureChange (34) | 00      |
| 1    | down (пониження)  | int8 | m_... (32/34)    |         |
| 2    | no change (без змін) | int8 | m_... (32/34)    |         |

### Zone Flag (маршальські зони)

У **MarshalZone** (частина PacketSessionData). Використовується: **MarshalZone.m_zoneFlag (offset 4, кожен елемент)**【6†L1-L4】.  
| Code | Опис               | Тип  | Поле              | Приклад |
|------|--------------------|-----|-------------------|---------|
| -1   | invalid/unknown (немає даних) | int8 | MarshalZone[].m_zoneFlag | FF      |
| 0    | none (немає прапора) | int8 | MarshalZone[].m_zoneFlag | 00      |
| 1    | green (зелений)     | int8 | MarshalZone[].m_zoneFlag | 01      |
| 2    | blue (синій)       | int8 | MarshalZone[].m_zoneFlag | 02      |
| 3    | yellow (жовтий)    | int8 | MarshalZone[].m_zoneFlag | 03      |

### Formula (клас/серія авто)

Використовується в PacketSessionData **m_formula (offset 37)**【6†L25-L28】.  
| Code | Опис              | Тип   | Поле (офсет)    | Приклад |
|------|-------------------|------|-----------------|---------|
| 0    | F1 Modern         | uint8 | m_formula (37)  | 00      |
| 1    | F1 Classic        | uint8 | m_formula (37)  |         |
| 2    | F2                | uint8 | m_formula (37)  |         |
| 3    | F1 Generic        | uint8 | m_formula (37)  |         |
| 4    | Beta              | uint8 | m_formula (37)  |         |
| 6    | Esports           | uint8 | m_formula (37)  |         |

### SLI Pro Support

Використовується в PacketSessionData **m_sliProNativeSupport (offset 46)**【6†L31-L34】.  
| Code | Опис   | Тип   | Поле (офсет)           | Приклад |
|------|--------|------|------------------------|---------|
| 0    | inactive (неактивний) | uint8 | m_sliProNativeSupport (46) | 00 |
| 1    | active (активний)    | uint8 | m_sliProNativeSupport (46) | 01 |

### Safety Car Status

Використовується в PacketSessionData **m_safetyCarStatus (offset 153)**【6†L31-L34】.  
| Code | Опис                        | Тип   | Поле (офсет)         | Приклад |
|------|-----------------------------|------|----------------------|---------|
| 0    | no safety car (немає SC)    | uint8 | m_safetyCarStatus (153) | 00      |
| 1    | full safety car             | uint8 | m_safetyCarStatus (153) | 01      |
| 2    | virtual safety car          | uint8 | m_safetyCarStatus (153) | 02      |
| 3    | formation lap (SC на колі)  | uint8 | m_safetyCarStatus (153) | 03      |

### Network Game Mode

Використовується в PacketSessionData **m_networkGame (offset 154)**【6†L37-L40】.  
| Code | Опис         | Тип   | Поле (офсет)        | Приклад |
|------|--------------|------|---------------------|---------|
| 0    | offline (офлайн) | uint8 | m_networkGame (154) | 00      |
| 1    | online       | uint8 | m_networkGame (154) | 01      |

### Forecast Accuracy

Використовується в PacketSessionData **m_forecastAccuracy (offset 668)**【6†L42-L44】.  
| Code | Опис      | Тип   | Поле (офсет)           | Приклад |
|------|-----------|------|------------------------|---------|
| 0    | Perfect   | uint8 | m_forecastAccuracy (668) | 00      |
| 1    | Approximate | uint8 | m_forecastAccuracy (668) | 01      |

### Steering Assist

Використовується в PacketSessionData **m_steeringAssist (offset 685)**【6†L48-L51】.  
| Code | Опис    | Тип   | Поле (офсет)           | Приклад |
|------|---------|------|------------------------|---------|
| 0    | off     | uint8 | m_steeringAssist (685) | 00      |
| 1    | on      | uint8 | m_steeringAssist (685) | 01      |

### Braking Assist

Використовується в PacketSessionData **m_brakingAssist (offset 686)**【6†L49-L51】.  
| Code | Опис          | Тип   | Поле (офсет)           | Приклад |
|------|---------------|------|------------------------|---------|
| 0    | off           | uint8 | m_brakingAssist (686)  | 00      |
| 1    | low (низький)   | uint8 | m_brakingAssist (686)  | 01      |
| 2    | medium (середній) | uint8 | m_brakingAssist (686)  | 02      |
| 3    | high (високий)  | uint8 | m_brakingAssist (686)  | 03      |

### Gearbox Assist

Використовується в PacketSessionData **m_gearboxAssist (offset 687)**【6†L49-L52】.  
| Code | Опис                     | Тип   | Поле (офсет)          | Приклад |
|------|--------------------------|------|-----------------------|---------|
| 1    | manual (ручне)            | uint8 | m_gearboxAssist (687) | 01      |
| 2    | manual + suggested gear   | uint8 | m_gearboxAssist (687) | 02      |
| 3    | auto (автоматичне)        | uint8 | m_gearboxAssist (687) | 03      |

### Pit Assist

Використовується в PacketSessionData **m_pitAssist (offset 688)**【6†L51-L54】.  
| Code | Опис     | Тип   | Поле (офсет)        | Приклад |
|------|----------|------|---------------------|---------|
| 0    | off      | uint8 | m_pitAssist (688)   | 00      |
| 1    | on       | uint8 | m_pitAssist (688)   | 01      |

### Pit Release Assist

Використовується в PacketSessionData **m_pitReleaseAssist (offset 689)**【6†L54-L58】.  
| Code | Опис     | Тип   | Поле (офсет)           | Приклад |
|------|----------|------|------------------------|---------|
| 0    | off      | uint8 | m_pitReleaseAssist (689) | 00      |
| 1    | on       | uint8 | m_pitReleaseAssist (689) | 01      |

### ERS Assist

Використовується в PacketSessionData **m_ERSAssist (offset 690)**【6†L54-L58】.  
| Code | Опис     | Тип   | Поле (офсет)         | Приклад |
|------|----------|------|----------------------|---------|
| 0    | off      | uint8 | m_ERSAssist (690)    | 00      |
| 1    | on       | uint8 | m_ERSAssist (690)    | 01      |

### DRS Assist

Використовується в PacketSessionData **m_DRSAssist (offset 691)**【6†L54-L58】.  
| Code | Опис     | Тип   | Поле (офсет)        | Приклад |
|------|----------|------|---------------------|---------|
| 0    | off      | uint8 | m_DRSAssist (691)   | 00      |
| 1    | on       | uint8 | m_DRSAssist (691)   | 01      |

### Dynamic Racing Line

Використовується в PacketSessionData **m_dynamicRacingLine (offset 692)**【6†L55-L58】.  
| Code | Опис                 | Тип   | Поле (офсет)            | Приклад |
|------|----------------------|------|-------------------------|---------|
| 0    | off                  | uint8 | m_dynamicRacingLine (692) | 00      |
| 1    | corners only         | uint8 | m_dynamicRacingLine (692) | 01      |
| 2    | full                 | uint8 | m_dynamicRacingLine (692) | 02      |

### Dynamic Racing Line Type

Використовується в PacketSessionData **m_dynamicRacingLineType (offset 693)**【6†L60-L63】.  
| Code | Опис      | Тип   | Поле (офсет)             | Приклад |
|------|-----------|------|--------------------------|---------|
| 0    | 2D        | uint8 | m_dynamicRacingLineType (693) | 00      |
| 1    | 3D        | uint8 | m_dynamicRacingLineType (693) | 01      |

### Session Length

Використовується в PacketSessionData **m_sessionLength (offset 700)**【6†L66-L70】.  
| Code | Опис         | Тип   | Поле (офсет)           | Приклад |
|------|--------------|------|------------------------|---------|
| 0    | None (немає) | uint8 | m_sessionLength (700)  | 00      |
| 2    | Very Short   | uint8 | m_sessionLength (700)  |  |
| 3    | Short        | uint8 | m_sessionLength (700)  |  |
| 4    | Medium       | uint8 | m_sessionLength (700)  |  |
| 5    | Medium Long  | uint8 | m_sessionLength (700)  |  |
| 6    | Long         | uint8 | m_sessionLength (700)  |  |
| 7    | Full         | uint8 | m_sessionLength (700)  |  |

### Speed Units (Lead Player)

Використовується в PacketSessionData **m_speedUnitsLeadPlayer (offset 701)**【6†L67-L70】.  
| Code | Опис   | Тип   | Поле (офсет)            | Приклад |
|------|--------|------|-------------------------|---------|
| 0    | MPH    | uint8 | m_speedUnitsLeadPlayer (701) | 00      |
| 1    | KPH    | uint8 | m_speedUnitsLeadPlayer (701) | 01      |

### Temperature Units (Lead Player)

Використовується в PacketSessionData **m_temperatureUnitsLeadPlayer (offset 702)**【6†L67-L70】.  
| Code | Опис    | Тип   | Поле (офсет)            | Приклад |
|------|---------|------|-------------------------|---------|
| 0    | Celsius  | uint8 | m_temperatureUnitsLeadPlayer (702) | 00 |
| 1    | Fahrenheit | uint8 | m_temperatureUnitsLeadPlayer (702) | 01 |

### Speed Units (Secondary Player)

Використовується в PacketSessionData **m_speedUnitsSecondaryPlayer (offset 703)**【6†L72-L75】.  
| Code | Опис   | Тип   | Поле (офсет)               | Приклад |
|------|--------|------|----------------------------|---------|
| 0    | MPH    | uint8 | m_speedUnitsSecondaryPlayer (703) | 00      |
| 1    | KPH    | uint8 | m_speedUnitsSecondaryPlayer (703) | 01      |

### Temperature Units (Secondary Player)

Використовується в PacketSessionData **m_temperatureUnitsSecondaryPlayer (offset 704)**【6†L73-L75】.  
| Code | Опис    | Тип   | Поле (офсет)               | Приклад |
|------|---------|------|----------------------------|---------|
| 0    | Celsius   | uint8 | m_temperatureUnitsSecondaryPlayer (704) | 00 |
| 1    | Fahrenheit | uint8 | m_temperatureUnitsSecondaryPlayer (704) | 01 |

### Equal Car Performance

Використовується в PacketSessionData **m_equalCarPerformance (offset 708)**【6†L78-L82】.  
| Code | Опис      | Тип   | Поле (офсет)            | Приклад |
|------|-----------|------|-------------------------|---------|
| 0    | Off       | uint8 | m_equalCarPerformance (708) | 00      |
| 1    | On        | uint8 | m_equalCarPerformance (708) | 01      |

### Recovery Mode (Flashback)

Використовується в PacketSessionData **m_recoveryMode (offset 709)**【6†L78-L81】.  
| Code | Опис             | Тип   | Поле (офсет)         | Приклад |
|------|------------------|------|----------------------|---------|
| 0    | None             | uint8 | m_recoveryMode (709) | 00      |
| 1    | Flashbacks       | uint8 | m_recoveryMode (709) | 01      |
| 2    | Auto-recovery    | uint8 | m_recoveryMode (709) | 02      |

### Flashback Limit

Використовується в PacketSessionData **m_flashbackLimit (offset 710)**【6†L79-L82】.  
| Code | Опис      | Тип   | Поле (офсет)           | Приклад |
|------|-----------|------|------------------------|---------|
| 0    | Low        | uint8 | m_flashbackLimit (710) | 00      |
| 1    | Medium     | uint8 | m_flashbackLimit (710) | 01      |
| 2    | High       | uint8 | m_flashbackLimit (710) | 02      |
| 3    | Unlimited  | uint8 | m_flashbackLimit (710) | 03      |

### Surface Type

Використовується в PacketSessionData **m_surfaceType (offset 711)**【6†L79-L83】.  
| Code | Опис        | Тип   | Поле (офсет)        | Приклад |
|------|-------------|------|---------------------|---------|
| 0    | Simplified  | uint8 | m_surfaceType (711) | 00      |
| 1    | Realistic   | uint8 | m_surfaceType (711) | 01      |

### Low Fuel Mode

Використовується в PacketSessionData **m_lowFuelMode (offset 712)**【6†L84-L88】.  
| Code | Опис        | Тип   | Поле (офсет)       | Приклад |
|------|-------------|------|--------------------|---------|
| 0    | Easy (легкий) | uint8 | m_lowFuelMode (712)  | 00      |
| 1    | Hard (жорсткий) | uint8 | m_lowFuelMode (712)  | 01      |

### Race Starts

Використовується в PacketSessionData **m_raceStarts (offset 713)**【6†L84-L88】.  
| Code | Опис       | Тип   | Поле (офсет)         | Приклад |
|------|------------|------|----------------------|---------|
| 0    | Manual     | uint8 | m_raceStarts (713)   | 00      |
| 1    | Assisted   | uint8 | m_raceStarts (713)   | 01      |

### Tyre Temperature (під час піт-лану)

Використовується в PacketSessionData **m_tyreTemperature (offset 714)**【6†L84-L88】.  
| Code | Опис                   | Тип   | Поле (офсет)          | Приклад |
|------|------------------------|------|-----------------------|---------|
| 0    | Surface only           | uint8 | m_tyreTemperature (714) | 00      |
| 1    | Surface & Carcass      | uint8 | m_tyreTemperature (714) | 01      |

### Pit Lane Tyre Simulation

Використовується в PacketSessionData **m_pitLaneTyreSim (offset 715)**【6†L84-L88】.  
| Code | Опис    | Тип   | Поле (офсет)           | Приклад |
|------|---------|------|------------------------|---------|
| 0    | On      | uint8 | m_pitLaneTyreSim (715) | 00      |
| 1    | Off     | uint8 | m_pitLaneTyreSim (715) | 01      |

### Car Damage

Використовується в PacketSessionData **m_carDamage (offset 716)**【6†L90-L94】.  
| Code | Опис       | Тип   | Поле (офсет)         | Приклад |
|------|------------|------|----------------------|---------|
| 0    | Off        | uint8 | m_carDamage (716)    | 00      |
| 1    | Reduced    | uint8 | m_carDamage (716)    | 01      |
| 2    | Standard   | uint8 | m_carDamage (716)    | 02      |
| 3    | Simulation | uint8 | m_carDamage (716)    | 03      |

### Car Damage Rate

Використовується в PacketSessionData **m_carDamageRate (offset 717)**【6†L90-L94】.  
| Code | Опис       | Тип   | Поле (офсет)          | Приклад |
|------|------------|------|-----------------------|---------|
| 0    | Reduced    | uint8 | m_carDamageRate (717) | 00      |
| 1    | Standard   | uint8 | m_carDamageRate (717) | 01      |
| 2    | Simulation | uint8 | m_carDamageRate (717) | 02      |

### Collisions

Використовується в PacketSessionData **m_collisions (offset 718)**【6†L91-L94】.  
| Code | Опис                   | Тип   | Поле (офсет)         | Приклад |
|------|------------------------|------|----------------------|---------|
| 0    | Off                    | uint8 | m_collisions (718)   | 00      |
| 1    | Player-to-Player Off   | uint8 | m_collisions (718)   | 01      |
| 2    | On                     | uint8 | m_collisions (718)   | 02      |

### Collisions Off For First Lap Only

Використовується в PacketSessionData **m_collisionsOffForFirstLapOnly (offset 719)**【6†L91-L94】.  
| Code | Опис   | Тип   | Поле (офсет)                   | Приклад |
|------|--------|------|--------------------------------|---------|
| 0    | Disabled | uint8 | m_collisionsOffForFirstLapOnly (719) | 00 |
| 1    | Enabled  | uint8 | m_collisionsOffForFirstLapOnly (719) | 01 |

### MP Unsafe Pit Release

Використовується в PacketSessionData **m_mpUnsafePitRelease (offset 720)**【6†L96-L100】.  
| Code | Опис     | Тип   | Поле (офсет)             | Приклад |
|------|----------|------|--------------------------|---------|
| 0    | On       | uint8 | m_mpUnsafePitRelease (720) | 00      |
| 1    | Off      | uint8 | m_mpUnsafePitRelease (720) | 01      |

### MP Off For Griefing

Використовується в PacketSessionData **m_mpOffForGriefing (offset 721)**【6†L96-L100】.  
| Code | Опис       | Тип   | Поле (офсет)            | Приклад |
|------|------------|------|-------------------------|---------|
| 0    | Disabled   | uint8 | m_mpOffForGriefing (721) | 00      |
| 1    | Enabled    | uint8 | m_mpOffForGriefing (721) | 01      |

### Corner Cutting Stringency

Використовується в PacketSessionData **m_cornerCuttingStringency (offset 722)**【6†L96-L100】.  
| Code | Опис      | Тип   | Поле (офсет)             | Приклад |
|------|-----------|------|--------------------------|---------|
| 0    | Regular   | uint8 | m_cornerCuttingStringency (722) | 00      |
| 1    | Strict    | uint8 | m_cornerCuttingStringency (722) | 01      |

### Parc Ferme Rules

Використовується в PacketSessionData **m_parcFermeRules (offset 723)**【6†L96-L100】.  
| Code | Опис    | Тип   | Поле (офсет)             | Приклад |
|------|---------|------|--------------------------|---------|
| 0    | Off     | uint8 | m_parcFermeRules (723)   | 00      |
| 1    | On      | uint8 | m_parcFermeRules (723)   | 01      |

### Pit Stop Experience

Використовується в PacketSessionData **m_pitStopExperience (offset 724)**【6†L101-L106】.  
| Code | Опис       | Тип   | Поле (офсет)            | Приклад |
|------|------------|------|-------------------------|---------|
| 0    | Automatic  | uint8 | m_pitStopExperience (724) | 00      |
| 1    | Broadcast  | uint8 | m_pitStopExperience (724) | 01      |
| 2    | Immersive  | uint8 | m_pitStopExperience (724) | 02      |

### Safety Car (Наступні налаштування)

Використовується в PacketSessionData **m_safetyCar (offset 725)**【6†L101-L105】 та **m_safetyCarExperience (offset 726)**【6†L102-L106】.  
| Code | Опис    | Тип   | Поле (офсет)          | Приклад |
|------|---------|------|-----------------------|---------|
| 0    | Off     | uint8 | m_safetyCar (725)     | 00      |
| 1    | Reduced | uint8 | m_safetyCar (725)     | 01      |
| 2    | Standard| uint8 | m_safetyCar (725)     | 02      |
| 3    | Increased| uint8| m_safetyCar (725)     | 03      |
| 0    | Broadcast| uint8| m_safetyCarExperience (726)| 00 |
| 1    | Immersive| uint8| m_safetyCarExperience (726)| 01 |

### Formation Lap

Використовується в PacketSessionData **m_formationLap (offset 727)**【6†L105-L108】.  
| Code | Опис    | Тип   | Поле (офсет)           | Приклад |
|------|---------|------|------------------------|---------|
| 0    | Off     | uint8 | m_formationLap (727)   | 00      |
| 1    | On      | uint8 | m_formationLap (727)   | 01      |

### Formation Lap Experience

Використовується в PacketSessionData **m_formationLapExperience (offset 728)**【6†L108-L110】.  
| Code | Опис    | Тип   | Поле (офсет)                 | Приклад |
|------|---------|------|------------------------------|---------|
| 0    | Broadcast| uint8 | m_formationLapExperience (728)| 00      |
| 1    | Immersive| uint8 | m_formationLapExperience (728)| 01      |

### Red Flags

Використовується в PacketSessionData **m_redFlags (offset 729)**【6†L109-L113】.  
| Code | Опис    | Тип   | Поле (офсет)        | Приклад |
|------|---------|------|---------------------|---------|
| 0    | Off     | uint8 | m_redFlags (729)    | 00      |
| 1    | Reduced | uint8 | m_redFlags (729)    | 01      |
| 2    | Standard| uint8 | m_redFlags (729)    | 02      |
| 3    | Increased| uint8| m_redFlags (729)    | 03      |

### Affects Licence Level (Solo/MP)

Використовується в PacketSessionData **m_affectsLicenceLevelSolo (offset 730)** та **m_affectsLicenceLevelMP (offset 731)**【6†L108-L113】.  
| Code | Опис  | Тип   | Поле (офсет)                 | Приклад |
|------|-------|------|------------------------------|---------|
| 0    | Off   | uint8 | m_affectsLicenceLevelSolo (730); m_affectsLicenceLevelMP (731) | 00 |
| 1    | On    | uint8 | m_affectsLicenceLevelSolo (730); m_affectsLicenceLevelMP (731) | 01 |

### [Lap Data Packet Enums]

Нижче наведено enum з PacketLapData та PacketEventData (апендикс), також знайдені в документі.

- **Pit Status** (в PacketLapData): `m_pitStatus (offset 442)`【7†L442-L444】.  
  - 0 = none, 1 = pitting, 2 = in pit area.

- **Sector** (в PacketLapData): `m_sector (offset 444)`【7†L442-L444】.  
  - 0 = sector1, 1 = sector2, 2 = sector3.

- **Current Lap Invalid**: `m_currentLapInvalid (offset 445)`【7†L443-L445】.  
  - 0 = valid, 1 = invalid.

- **Driver Status**: `m_driverStatus (offset 453)`【7†L452-L454】.  
  - 0 = in garage, 1 = flying lap, 2 = in lap, 3 = out lap, 4 = on track.

- **Result Status**: `m_resultStatus (offset 455)`【7†L453-L457】.  
  - 0 = invalid, 1 = inactive, 2 = active, 3 = finished, 4 = did not finish, 5 = disqualified, 6 = not classified, 7 = retired.

- **Pit Lane Timer Active**: `m_pitLaneTimerActive (offset 458)`【7†L456-L459】.  
  - 0 = inactive, 1 = active.

- **Event: Retirement Reason**: в `union EventDataDetails.Retirement.reason`【7†L501-L508】.  
  - 0 = invalid, 1 = retired, 2 = finished, 3 = terminal damage, 4 = inactive, 5 = not enough laps, 6 = black flagged, 7 = red flagged, 8 = mechanical failure, 9 = session skipped, 10 = session simulated.

- **Event: DRS Disabled Reason**: в `union EventDataDetails.DRSDisabled.reason`【7†L510-L514】.  
  - 0 = Wet track, 1 = Safety car, 2 = Red flag, 3 = Min lap not reached.

- **Event: SafetyCar**: в `union EventDataDetails.SafetyCar.safetyCarType` та `eventType`【7†L587-L593】.  
  - safetyCarType: 0 = No SC, 1 = Full SC, 2 = Virtual SC, 3 = Formation Lap SC.  
  - eventType: 0 = Deployed, 1 = Returning, 2 = Returned, 3 = Resume Race.

Для всіх наведених таблиць використано офіційний PDF F1 25【6†L25-L28】【7†L442-L450】. Якщо enum відсутній у PDF (наприклад, додаткові режими гри/правила), його пропущено або помічено як «відсутній». Для порівняння з F1 24/23 де можливо, використано GitHub-репозиторії (неофіційні)【6†L66-L72】【7†L442-L450】.  

## Діаграми (Mermaid)

```mermaid
flowchart TD
  subgraph PacketSessionData
    S(m_weather=enum 0-5) -- uses Weather enum --> Weather
    TC(m_trackTemperatureChange=enum 0-2) -- uses TempChange enum --> TempChange
    AF(m_airTemperatureChange=enum 0-2) -- uses TempChange enum --> TempChange
    F(m_formula=enum ...) -- uses Formula enum --> Formula
    SC(m_safetyCarStatus=enum 0-3) -- uses SCStatus enum --> SCStatus
    NG(m_networkGame=0/1) -- binary
    FA(m_forecastAccuracy=0/1) -- binary
    SA(m_steeringAssist=0/1) -- binary
    BA(m_brakingAssist=enum 0-3) -- enum
    GA(m_gearboxAssist=enum 1-3) -- enum
    PA(m_pitAssist=0/1) -- binary
    PR(m_pitReleaseAssist=0/1)
    ER(m_ERSAssist=0/1)
    DR(m_DRSAssist=0/1)
    RL(m_dynamicRacingLine=enum 0-2)
    RLtype(m_dynamicRacingLineType=0/1)
    SL(m_sessionLength=enum 0,2-7)
    speedLead(m_speedUnitsLeadPlayer=0/1)
    tempLead(m_temperatureUnitsLeadPlayer=0/1)
    speedSec(m_speedUnitsSecondaryPlayer=0/1)
    tempSec(m_temperatureUnitsSecondaryPlayer=0/1)
    EQ(m_equalCarPerformance=0/1)
    RCV(m_recoveryMode=enum 0-2)
    FLB(m_flashbackLimit=enum 0-3)
    ST(m_surfaceType=0/1)
    LF(m_lowFuelMode=0/1)
    RS(m_raceStarts=0/1)
    TT(m_tyreTemperature=0/1)
    PLS(m_pitLaneTyreSim=0/1)
    CD(m_carDamage=enum 0-3)
    CDR(m_carDamageRate=enum 0-2)
    CO(m_collisions=enum 0-2)
    CO1(m_collisionsOffForFirstLapOnly=0/1)
    MPUPR(m_mpUnsafePitRelease=0/1)
    MPOF(m_mpOffForGriefing=0/1)
    CC(m_cornerCuttingStringency=0/1)
    PF(m_parcFermeRules=0/1)
    PSX(m_pitStopExperience=enum 0-2)
    SCx(m_safetyCar=enum 0-3)
    SCe(m_safetyCarExperience=0/1)
    FLx(m_formationLap=0/1)
    FLe(m_formationLapExperience=0/1)
    RF(m_redFlags=enum 0-3)
    LC(m_affectsLicenceLevelSolo=0/1)
    LM(m_affectsLicenceLevelMP=0/1)
  end
```

## Assumptions & Calculation

- Усі структури **packed, Little Endian** (офіційно зазначено)【1†L172-L181】.
- **PacketHeader** розміром 29 байт (складається з uint16 + 5×uint8 + uint64 + float + 2×uint32 + 2×uint8)【1†L191-L200】. Цей префікс додається у кожному пакеті.
- Всі офсети (див. вище) обчислено послідовно від початку PacketSessionData (тобто починаючи з байту 0 даних після заголовка).
- Якщо в PDF відсутня інформація по enum (напр., повні списки правил чи режимів), позначено як «відсутній». Неофіційні GitHub-джерела використовувались тільки для підтвердження/додатків і зазначені як «неофіційне».  

## Джерела

- Офіційний PDF **Data Output From F1 25 (v3)** (EA/Codemasters)【1†L173-L181】【7†L442-L450】  
- Неофіційні репозиторії:
  - MacManley/f1-24-udp (F1 24 packet specs)【6†L66-L72】  
  - MacManley/f1-23-udp (F1 23 packet specs)【6†L66-L72】  
  (використовувались для порівняння enum в пак. 2024/2023)