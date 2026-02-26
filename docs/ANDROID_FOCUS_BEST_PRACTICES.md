# Android 焦点处理最佳实践

本文档总结了 Android 开发中焦点（Focus）处理的常见问题和解决方案，适用于 TV 应用、键盘导航场景。

## 常见问题

### 1. 自定义背景 selector 不生效

**现象**：定义了 `state_focused` 的 selector，但按钮获得焦点时没有显示预期的效果。

**原因**：Material Components 的 `Button` 会自动应用 `backgroundTint`，覆盖自定义背景。

**解决方案**：使用 `androidx.appcompat.widget.AppCompatButton` 替代 `Button`。

```xml
<!-- 错误：焦点样式不生效 -->
<Button
    android:id="@+id/btnRecord"
    android:background="@drawable/btn_selector"
    ... />

<!-- 正确：焦点样式生效 -->
<androidx.appcompat.widget.AppCompatButton
    android:id="@+id/btnRecord"
    android:background="@drawable/btn_selector"
    ... />
```

### 2. 焦点边框颜色与按钮颜色接近

**现象**：焦点边框颜色和按钮颜色太接近，看不清楚焦点在哪里。

**解决方案**：使用对比色。

| 按钮颜色 | 推荐焦点颜色 |
|---------|-------------|
| 红色 (#E53935) | 黄色 (#FFEB3B) |
| 绿色 (#43A047) | 黄色 (#FFEB3B) |
| 蓝色 (#1E88E5) | 橙色 (#FF9800) |
| 紫色 (#6200EE) | 黄色 (#FFEB3B) 或 橙色 |
| 橙色 (#FB8C00) | 蓝色 (#2196F3) |

### 3. 焦点边框太细看不清

**解决方案**：使用 `layer-list` 实现粗边框。

```xml
<!-- res/drawable/btn_selector.xml -->
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 焦点状态 -->
    <item android:state_focused="true">
        <layer-list>
            <!-- 外层：焦点边框色 -->
            <item>
                <shape android:shape="rectangle">
                    <solid android:color="#FFEB3B" /> <!-- 黄色边框 -->
                    <corners android:radius="16dp" />
                </shape>
            </item>
            <!-- 内层：按钮本色，带 padding 形成边框 -->
            <item android:left="6dp" android:right="6dp"
                  android:top="6dp" android:bottom="6dp">
                <shape android:shape="rectangle">
                    <solid android:color="#E53935" /> <!-- 红色按钮 -->
                    <corners android:radius="10dp" />
                </shape>
            </item>
        </layer-list>
    </item>

    <!-- 按下状态 -->
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#B71C1C" />
            <corners android:radius="14dp" />
        </shape>
    </item>

    <!-- 默认状态 -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#E53935" />
            <corners android:radius="14dp" />
        </shape>
    </item>
</selector>
```

## ListView/RecyclerView 焦点处理

### 1. 列表项焦点样式

```xml
<!-- res/drawable/item_selector.xml -->
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="#FFF9C4" /> <!-- 浅黄背景 -->
            <stroke android:width="4dp" android:color="#FFC107" /> <!-- 黄色边框 -->
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#FFF59D" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item android:state_activated="true">
        <shape android:shape="rectangle">
            <solid android:color="#C8E6C9" /> <!-- 选中背景 -->
            <stroke android:width="2dp" android:color="#43A047" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <corners android:radius="8dp" />
        </shape>
    </item>
</selector>
```

### 2. 监听键盘确认键播放

```kotlin
listView.setOnKeyListener { _, keyCode, event ->
    if (event.action == KeyEvent.ACTION_DOWN) {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                val position = listView.selectedItemPosition
                if (position != ListView.INVALID_POSITION) {
                    // 执行操作，如播放
                    playItemAtPosition(position)
                }
                true
            }
            else -> false
        }
    } else {
        false
    }
}
```

### 3. 跟踪焦点变化

```kotlin
listView.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // 更新 UI 显示当前焦点项
        updateSelection(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        clearSelection()
    }
})
```

## 焦点导航配置

### 1. XML 中配置导航顺序

```xml
<Button
    android:id="@+id/btnRecord"
    android:nextFocusDown="@id/btnPlay"
    android:nextFocusUp="@id/btnStop" />

<Button
    android:id="@+id/btnPlay"
    android:nextFocusDown="@id/listView"
    android:nextFocusUp="@id/btnRecord" />
```

### 2. 代码中配置

```kotlin
btnRecord.nextFocusDownId = R.id.btnPlay
btnRecord.nextFocusUpId = R.id.btnStop
```

## 完整示例

### colors.xml

```xml
<resources>
    <!-- 按钮颜色 -->
    <color name="record_button">#E53935</color>
    <color name="play_button">#43A047</color>
    <color name="stop_button">#FB8C00</color>

    <!-- 焦点颜色 - 使用高对比色 -->
    <color name="focus_border">#FFEB3B</color>      <!-- 明亮黄色 -->
    <color name="focus_border_thick">#FFC107</color> <!-- 深黄色 -->

    <!-- 列表项焦点 -->
    <color name="item_focused">#FFF9C4</color>     <!-- 浅黄背景 -->
    <color name="item_selected">#C8E6C9</color>     <!-- 浅绿选中 -->
</resources>
```

### 布局文件

```xml
<!-- 使用 AppCompatButton -->
<androidx.appcompat.widget.AppCompatButton
    android:id="@+id/btnAction"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:background="@drawable/btn_selector"
    android:textColor="@android:color/white"
    android:focusable="true"
    android:focusableInTouchMode="true" />
```

## 调试技巧

### 1. 查看当前焦点 View

```kotlin
// 在 Activity 中
val focusedView = currentFocus
Log.d("Focus", "Focused view: ${focusedView?.id}")
```

### 2. 请求焦点

```kotlin
btnRecord.requestFocus()
// 或
btnRecord.post { btnRecord.requestFocus() }
```

### 3. 强制显示焦点

```kotlin
// 在主题中设置
<style name="MyTheme" parent="Theme.AppCompat">
    <item name="android:focusable">@bool/true</item>
</style>
```

## 检查清单

发布前检查：

- [ ] 所有可交互控件设置了 `focusable="true"`
- [ ] 使用 `AppCompatButton` 而非 `Button`
- [ ] 焦点边框颜色与按钮颜色有明显对比
- [ ] 焦点边框宽度足够（建议 4-6dp）
- [ ] 配置了 `nextFocusUp/Down/Left/Right`
- [ ] 测试键盘/遥控器导航
- [ ] 测试触摸和键盘切换时焦点状态正确

## 参考资料

- [Android Focus Handling](https://developer.android.com/develop/ui/views/touch-and-input/focus-handling)
- [TV App Navigation](https://developer.android.com/training/tv/start/navigation)
- [StateListDrawable](https://developer.android.com/reference/android/graphics/drawable/StateListDrawable)