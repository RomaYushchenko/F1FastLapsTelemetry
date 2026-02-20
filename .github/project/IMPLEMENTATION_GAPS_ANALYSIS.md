# Аналіз прогалин: документація vs реалізація (Backend / Frontend)

> **Мета:** пояснити, чому деякі endpoint'и були відсутні на бекенді при наявності викликів на фронті, та зафіксувати виявлену логіку, що не була реалізована.

---

## 1. Чому ендпоінти `/pace` та `/trace` були відсутні

### 1.1 Розсинхронізація документів

| Джерело | Що сказано | Наслідок |
|--------|------------|----------|
| **rest_web_socket_api_contracts_f_1_telemetry.md** | У § 3 описані лише: `GET /sessions`, `GET /sessions/{uid}`, `GET .../laps`, `GET .../summary`. Endpoint'ів `/pace` та `/trace` **немає**. | Бекенд-розробка орієнтувалася на цей контракт; ці два endpoint'и не потрапили в список завдань Етапу 8. |
| **implementation_steps_plan.md** (Етап 8) | Кроки 8.1–8.7: sessions, laps, sectors, summary, state, telemetry (skipped). **8.8 pace та 8.9 trace не були додані.** | У чеклист REST API вони не входили, тому не реалізовувалися. |
| **frontend_refinement_plan_f1_telemetry.md** | Блок **«C. Наступна ітерація: діаграми темпу та педалей»** описує: `GET /api/sessions/{id}/pace` та `GET /api/sessions/{id}/laps/{lapNum}/trace` як **бекенд-підготовку для майбутнього**. | Endpoint'и описані як «план на потім», а не як обовʼязкова частина MVP. |
| **IMPLEMENTATION_PROGRESS.md** (Stage 11) | Пункт 11.C: «Pace & pedal trace charts \| `GET /api/sessions/{uid}/pace`, `/laps/{lap}/trace` \| ✅ Recharts-based…» | **Фронтенд** позначений як готовий (чарти викликають ці URL), але **бекенд** для цих URL не був у переліку Етапу 8. |
| **telemetry_diagrams_plan.md** (§ 4) | У таблиці «Історичні діаграми» є лише: summary, laps, sectors, опційно telemetry. **Pace chart і pedal trace не перелічені як окремі REST endpoint'и.** | План діаграм не вимагав явно endpoint'ів для pace/trace. |

### 1.2 Висновок

- **Офіційний REST-контракт** і **Етап 8** не включали `/pace` та `/trace`.
- **Фронтенд** (Етап 11) був реалізований з викликами цих endpoint'ів і позначений як ✅, тоді як **бекенд** для них не планувався в Етапі 8.
- **frontend_refinement_plan** описував ці endpoint'и як «наступну ітерацію», тому вони не потрапили в основний бекенд-чеклист.

**Підсумок:** Це класична прогалина «UI готовий, API не реалізований» через те, що контракт і план етапів не були оновлені після додавання на UI графіків pace та pedal trace.

---

## 2. Що було зроблено для усунення прогалини

1. **Бекенд**
   - Додано `GET /api/sessions/{id}/pace` (LapController): дані з таблиці `lap` → `PacePointDto[]`.
   - Додано `GET /api/sessions/{id}/laps/{lapNum}/trace` (LapController): спочатку MVP stub (200 + `[]`); далі реалізовано повне заповнення: запис у `car_telemetry_raw` (lap_number, lap_distance_m, throttle, brake) під час сесії та повернення точок з БД (див. **PEDAL_TRACE_FEATURE_ANALYSIS_AND_PLAN.md**).

2. **Документація**
   - У **rest_web_socket_api_contracts_f_1_telemetry.md** додано § 3.4 (Pace) та § 3.5 (Pedal trace).
   - У **implementation_steps_plan.md** додано кроки 8.8 та 8.9 з позначкою ✅.

---

## 3. Інша логіка з документації, яка не реалізована або частково реалізована

| Джерело | Опис | Статус на бекенді / фронті |
|--------|------|----------------------------|
| **rest_web_socket_api_contracts** § 3 | `GET /api/sessions/{uid}/telemetry?from=&to=&metric=` | Не реалізовано (опційно MVP, Skipped). |
| **frontend_refinement_plan** § C.1 | Pace DTO з полями `stintIndex`, `tyreCompound` | Зараз pace повертає лише `lapNumber`, `lapTimeMs`. Стинт/шини — майбутнє розширення. |
| **frontend_refinement_plan** § C.1 | Trace: масив `{ distance, throttle, brake }` з даних по колу | ✅ Реалізовано: RawTelemetryWriter записує семпли в car_telemetry_raw; GET trace повертає точки з БД по (session, car, lap). |
| **mvp-requirements.md** | Опційно: `GET /api/sessions/{sessionUid}/telemetry?from=&to=&metric=` | Не реалізовано. |
| **Session list** (контракт) | У контракті приклад використовує `sessionUID` (number); у реалізації використовується `id` (UUID public_id) | Реалізація узгоджена з поточним контрактом (public_id); в контракті можна уточнити підтримку обох ідентифікаторів. |

---

## 4. Рекомендації на майбутнє

1. **Один джерело істини для API:** при додаванні нового екрану/діаграми на фронті одразу додавати потрібні endpoint'и в **rest_web_socket_api_contracts_f_1_telemetry.md** і відповідні кроки в **implementation_steps_plan.md** (Етап 8 або інший).
2. **Перевірка контракту перед «UI ready»:** перед позначенням етапу UI як завершеного перевіряти, що всі викликані URL реалізовані на бекенді або явно позначені як optional з обробкою помилок на клієнті.
3. **Pace/trace далі:** pace — опційно розширити DTO (стінт/шини). **Pedal trace** — реалізовано: збереження raw car telemetry по колу (lap_number, lap_distance_m) та GET trace з БД; деталі в **PEDAL_TRACE_FEATURE_ANALYSIS_AND_PLAN.md**.
