#!/bin/bash

# Script to export all graphics from Nextcloud Talk Android for redesign
# Экспорт всей графики из Nextcloud Talk Android для редизайна

set -e

echo "🎨 Экспорт графики для дизайнера DreamTheme"
echo "=============================================="

# Цвета
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

function print_step() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

function print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Создаем папку для экспорта
EXPORT_DIR="graphics-for-designer"
mkdir -p "$EXPORT_DIR"

print_step "Создание структуры папок..."
mkdir -p "$EXPORT_DIR"/{icons,splash,drawables,vectors,logos,screenshots}

# Экспортируем иконки приложения
print_step "Экспорт иконок приложения..."
find app/src -name "ic_launcher*" -type f -exec cp {} "$EXPORT_DIR/icons/" \;
find app/src -name "launcher_*" -type f -exec cp {} "$EXPORT_DIR/icons/" \;

# Экспортируем drawable ресурсы
print_step "Экспорт drawable ресурсов..."
find app/src -path "*/drawable*/*" -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.svg" -o -name "*.xml" \) -exec cp {} "$EXPORT_DIR/drawables/" \;

# Экспортируем векторные ресурсы
print_step "Экспорт векторных ресурсов..."
find drawable_resources -name "*.svg" -type f -exec cp {} "$EXPORT_DIR/vectors/" \;

# Экспортируем mipmap ресурсы (иконки разных размеров)
print_step "Экспорт mipmap ресурсов..."
find app/src -path "*/mipmap*/*" -type f -exec cp {} "$EXPORT_DIR/icons/" \;

# Создаем список всех найденных файлов
print_step "Создание описания найденных файлов..."
cat > "$EXPORT_DIR/README.md" << 'EOF'
# Графические ресурсы Nextcloud Talk Android

## Структура экспорта

### 📱 /icons/
Иконки приложения для разных размеров экранов:
- `ic_launcher.xml` - основная иконка (векторная)
- `ic_launcher_foreground.xml` - передний план иконки
- `ic_launcher_background.xml` - фон иконки
- Растровые версии для разных разрешений (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)

### 🎨 /drawables/
Drawable ресурсы (кнопки, фоны, иконки):
- Векторные drawable (XML)
- Растровые изображения (PNG, JPG)
- State-dependent drawables

### 📐 /vectors/
Исходные векторные файлы (SVG):
- `icon-foreground.svg` - исходник переднего плана иконки
- `icon-background.svg` - исходник фона иконки

### 🖼️ /logos/
Логотипы и брендинг

### 📱 /screenshots/
Скриншоты текущего приложения для референса

## 🎯 Задачи для дизайнера

### Основная иконка приложения
- [ ] Новая иконка в стиле DreamTheme
- [ ] Adaptive icon (передний план + фон)
- [ ] Все размеры: 48dp, 72dp, 96dp, 144dp, 192dp
- [ ] Цветовая схема: #6366F1 (основной), #8B5CF6 (акцент)

### Splash screen
- [ ] Заставка при запуске приложения
- [ ] Анимированная иконка (Lottie)
- [ ] Совместимость с Android 12+ Splash Screen API

### UI иконки
- [ ] Иконки для всех функций мессенджера
- [ ] Стиль: минимализм, четкие линии
- [ ] Размеры: 24dp, 32dp (основные)
- [ ] Темная и светлая версии

### Дополнительно
- [ ] Фавикон для веб-версии
- [ ] Иконки для уведомлений
- [ ] Брендинг для экранов настроек

## 📏 Технические требования

### Форматы
- **Иконки**: SVG (исходники) + PNG (экспорт)
- **Векторы**: SVG
- **Растр**: PNG (с прозрачностью), JPG (без прозрачности)

### Размеры иконок Android
- **mdpi**: 48x48px (базовый)
- **hdpi**: 72x72px (1.5x)
- **xhdpi**: 96x96px (2x)
- **xxhdpi**: 144x144px (3x)
- **xxxhdpi**: 192x192px (4x)

### Цветовая палитра DreamTheme
```
#6366F1 - Основной (Indigo)
#8B5CF6 - Акцент (Purple)
#06B6D4 - Дополнительный (Cyan)
#F8FAFC - Фон светлый
#1E293B - Текст темный
```

## 📋 Чек-лист файлов для замены

### Обязательные файлы:
- [ ] `ic_launcher.xml`
- [ ] `ic_launcher_foreground.xml` 
- [ ] `ic_launcher_background.xml`
- [ ] `ic_launcher.png` (все размеры)
- [ ] `ic_launcher_round.png` (все размеры)

### Дополнительные иконки:
- [ ] Иконки меню и действий
- [ ] Иконки статусов
- [ ] Фоновые изображения

## 🚀 После готовности

1. Поместить все файлы в соответствующие папки проекта
2. Обновить файлы strings.xml с новым названием
3. Пересобрать приложение
4. Протестировать на разных устройствах

EOF

# Делаем скриншоты текущего приложения (если есть эмулятор)
if command -v adb &> /dev/null; then
    print_step "Попытка сделать скриншоты текущего приложения..."
    if adb devices | grep -q "device$"; then
        adb shell screencap -p > "$EXPORT_DIR/screenshots/current_app_$(date +%Y%m%d_%H%M).png" 2>/dev/null || true
    fi
fi

# Подсчитываем экспортированные файлы
ICON_COUNT=$(find "$EXPORT_DIR/icons" -type f | wc -l)
DRAWABLE_COUNT=$(find "$EXPORT_DIR/drawables" -type f | wc -l)
VECTOR_COUNT=$(find "$EXPORT_DIR/vectors" -type f | wc -l)

print_success "Экспорт завершен!"
echo ""
echo "📊 Статистика экспорта:"
echo "   Иконки: $ICON_COUNT файлов"
echo "   Drawables: $DRAWABLE_COUNT файлов"
echo "   Векторы: $VECTOR_COUNT файлов"
echo ""
echo "📁 Все файлы сохранены в: $EXPORT_DIR/"
echo "📖 Инструкции для дизайнера: $EXPORT_DIR/README.md"
echo ""
print_success "Готово! Можно передавать дизайнеру."
