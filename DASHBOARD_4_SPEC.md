# 🏎️ DASHBOARD 4: FULL DESIGN SPECIFICATION (DARK THEME)

Этот файл является эталоном оформления 4-го дашборда. В случае "затирания" темы или сброса стилей после Git Sync, использовать данные ниже для восстановления.

## 1. ЦВЕТОВАЯ ПАЛИТРА (Theme.kt / Color.kt)

### Базовые градиенты (AppColors):
- **BackgroundGradient:** `[0xFF0A0A0F, 0xFF12121A, 0xFF1A1A24]` (Вертикальный).
- **SurfaceGradient:** `[0x990A0A0F, 0x660A0A0F]` (Вертикальный для TopBar).
- **SurfaceOverlay:** `0x0AFFFFFF` (10% White, используется в TopBar container).
- **SurfaceMedium:** `0x15FFFFFF` (Подложки кнопок).

### Текст (Typography Colors):
- **TextPrimary:** `0xFFE0E0FF` (Светло-голубоватый белый).
- **TextSecondary:** `DarkBlueGrey80` (Основной серый).
- **TextTertiary:** `DarkBlueGrey40` (Темно-серый для меток).

### Акценты и Состояния:
- **PrimaryBlue:** `DarkBlue80` (`0xFF00D4FF`).
- **Success:** `0xFF4ADE80` (Зеленый).
- **Warning:** `0xFFFACC15` (Желтый).
- **Error:** `DarkPink80`.

---

## 2. TOPBAR (HomeScreen.kt)

- **Высота:** `COMPACT_TOP_BAR_HEIGHT` (32-40dp).
- **Фон:** `SurfaceGradient` + `containerColor = AppColors.SurfaceDark` (`0x0AFFFFFF`).
- **Заголовок:** "БОРТОВОЙ КОМПЬЮТЕР", иконка `DirectionsCar` (18.dp).
- **Кнопки:**
    - Контейнер: `IconButton` (36.dp).
    - Подложка: `CircleShape`, 30.dp, цвет `AppColors.SurfaceMedium`.
    - Иконка: 16.dp, цвет `AppColors.TextSecondary`.

---

## 3. ЦЕНТРАЛЬНЫЕ ПРИБОРЫ (DashboardType4Speedometer / CombinedGauge)

### Общие правила:
- **Цифры значений:** `Color.White`, размер `42f * unit`.
- **Единицы (км/ч, °C, л):** `Color.Gray`, размер `16f * unit`.
- **Внешнее кольцо:** `Color.White`, толщина `outerStrokeWidth`.
- **Внутреннее кольцо (центр):** `Color.Black` с тонкой обводкой `White.copy(alpha = 0.1f)`.

### Свечение (Glow):
- **DualRadialGlow:** Свечение вокруг кольца, пик яркости совпадает с радиусом шкалы.
- **Цвет свечения:** `geometry.ringColor` (динамический из настроек).

### Стрелки (Needles):
- **Тело:** Линия `1.5f * unit` (White) поверх широкой линии `5f * unit` (Accent Color 50% alpha).
- **Bloom:** Эффект "раскаленного" сегмента у основания (черный круг с радиальным градиентом акцентного цвета).

### Шлейф (Speedometer Trail):
- Динамический цвет по скорости: 
    - `0..30`: White
    - `30..60`: Green
    - `60..90`: Yellow
    - `90..130`: Orange
    - `130..170`: Red
    - `>170`: Burgundy (`0xFF800000`)

---

## 4. ВОЛЬТМЕТР (Нижняя дуга)

- **Диапазон:** 12.0V - 12.7V (базовый).
- **Индикация критического уровня:** `< 11.8V` -> Цвет `BRIGHT_RED` (`0xFFFF0000`) + анимация мигания.
- **Цветовая логика:** `voltageToColor()` (Синий -> Темно-зеленый -> Зеленый -> Желтый -> Красный -> Бордовый).

---

## 5. TRIP WIDGET (DashboardType4TripWidget)

### Границы и декор:
- **Верх:** Разделительная линия `1.5.dp` с горизонтальным градиентом (Alpha 0.1 -> 1.0 -> 0.1).
- **Низ:** Сплошная белая линия.
- **Бока:** Дуги `TripArc`, исчезающие кверху (Brush.sweepGradient).

### Контент:
- **Значения:** `Color.White.copy(alpha = 0.8f)`.
- **Метки (TRIP A, TOTAL):** `AppColors.TextTertiary`.
- **Топливный сегментный индикатор:**
    - 12 сегментов.
    - Активные: `alpha = 0.8f`.
    - Неактивные: `alpha = 0.15f`.
    - Разделитель (Gap): `1.5f * unit`.
- **Указатель-треугольник:** Привязан к `rangeProgress`, цвет `White 80%`.

---

## 6. ГЕОМЕТРИЯ (DashboardType4Geometry)

- Все размеры рассчитываются через коэффициент `unit = width / 400f`.
- Основной радиус шкалы: `scaleRadius`.
- Смещение текста: `textRadius`.
- Размеры рисок: `tickLarge`, `tickMedium`, `tickSmall`.
