$ErrorActionPreference = "Stop"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$repoRoot = Split-Path $PSScriptRoot -Parent
$assets = Join-Path $repoRoot "assets"
$tmp = Join-Path $assets "_capture"
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

function Adb { & $adb @args }
function Launch([string]$page) {
  Adb shell am force-stop com.arialentropy.kuiklytable | Out-Null
  Start-Sleep -Milliseconds 400
  Adb shell am start -n com.arialentropy.kuiklytable/.KuiklyRenderActivity --es pageName $page | Out-Null
  Start-Sleep -Seconds 2.5
}
function Shot([string]$name) {
  $raw = Join-Path $tmp "$name-raw.png"
  Adb exec-out screencap -p > $raw
  # status ~80, nav ~140 on 1080x2400 / 420dpi
  $out = Join-Path $assets $name
  ffmpeg -y -i $raw -vf "crop=1080:2180:0:80" $out 2>$null
  Write-Host "shot $name size=$((Get-Item $out).Length)"
}
function RecordGif([string]$name, [int]$seconds, [scriptblock]$actions) {
  $remote = "/sdcard/$name.mp4"
  Adb shell rm -f $remote 2>$null
  Start-Process -FilePath $adb -ArgumentList @("shell","screenrecord","--time-limit","$seconds",$remote) -NoNewWindow
  Start-Sleep -Milliseconds 800
  & $actions
  Start-Sleep -Seconds ($seconds + 1)
  $localMp4 = Join-Path $tmp "$name.mp4"
  Adb pull $remote $localMp4 | Out-Null
  $out = Join-Path $assets "$name.gif"
  # palette + fps 10, keep under ~10s of content
  ffmpeg -y -i $localMp4 -vf "fps=10,crop=1080:2180:0:80,scale=540:-1:flags=lanczos,palettegen" (Join-Path $tmp "$name-pal.png") 2>$null
  ffmpeg -y -i $localMp4 -i (Join-Path $tmp "$name-pal.png") -lavfi "fps=10,crop=1080:2180:0:80,scale=540:-1:flags=lanczos[x];[x][1:v]paletteuse" -loop 0 $out 2>$null
  Write-Host "gif $name size=$((Get-Item $out).Length)"
}

Write-Host "=== basic static ==="
Launch "table_basic"
Shot "table_showcase_basic.png"
# swipe up to zebra area
Adb shell input swipe 540 1600 540 700 400
Start-Sleep 1
Shot "_tmp_zebra.png"
# theme tab ~ x for 主题: tabs around y=220
# approximate: 基础~120, 滚动~280, 主题~440, 自定义~620, 状态~800
Adb shell input tap 440 230
Start-Sleep 1
Shot "table_showcase_theme.png"
Adb shell input tap 620 230
Start-Sleep 1
Shot "table_showcase_renderer.png"
Adb shell input tap 800 230
Start-Sleep 1
Shot "table_showcase_state.png"
Adb shell input tap 280 230
Start-Sleep 1
# scroll section - record short scroll gif
RecordGif "table_showcase_scroll_demo" 7 {
  Start-Sleep 1
  Adb shell input swipe 800 1400 200 1400 500
  Start-Sleep 1
  Adb shell input swipe 540 1700 540 900 600
  Start-Sleep 1
  Adb shell input swipe 200 1400 800 1400 500
}

Write-Host "=== data filter + large ==="
Launch "table_data"
Start-Sleep 1
Shot "table_datatable_selection.png"
# filter chips roughly below config - tap 在职 then 全部
RecordGif "table_datatable_filter" 8 {
  Start-Sleep 1
  # try several Y rows for filter chips
  foreach ($y in @(520, 580, 640, 700, 760)) {
    Adb shell input tap 220 $y  # 全部-ish
    Start-Sleep -Milliseconds 300
    Adb shell input tap 380 $y  # 在职
    Start-Sleep -Milliseconds 700
    Adb shell input tap 540 $y  # 休假
    Start-Sleep -Milliseconds 700
    Adb shell input tap 700 $y  # 离职
    Start-Sleep -Milliseconds 700
    Adb shell input tap 220 $y
    Start-Sleep -Milliseconds 400
  }
}

# large data tab
Adb shell input tap 540 230  # middle tab 大量数据 - need better coords
# tabs: 数据交互 / 大量数据 / 接入关系 - try x=540 for middle
Start-Sleep 2
RecordGif "table_showcase_large_demo" 8 {
  Start-Sleep 2
  Adb shell input swipe 540 1800 540 700 350
  Start-Sleep -Milliseconds 500
  Adb shell input swipe 540 1800 540 700 350
  Start-Sleep -Milliseconds 500
  Adb shell input swipe 540 700 540 1800 350
  Start-Sleep -Milliseconds 500
  Adb shell input swipe 540 1800 540 900 300
}

Write-Host "done"
