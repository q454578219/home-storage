# 鎴峰瀷鍥炬ā寮忥細瀹炵幇璁″垝

## 鍓嶇疆

- 纭 HomeStorageDatabase 褰撳墠 version 涓庣幇鏈?Migration 缁撴瀯锛堣浠ｇ爜锛?- 纭 MainActivity 瀵艰埅缁撴瀯锛坮oute 鍐欐硶锛夛紝鍐冲畾鏂板 route 鐨勬帴鍏ユ柟寮?- 澶嶇敤锛欼magePicker銆両mageStore銆丼potMarker 鎵嬪娍鎬濊矾銆丆reateCabinetScreen

## 瀹炵幇椤哄簭锛堟瘡姝ユ瀯寤洪獙璇侊級

### 闃舵 1锛氭暟鎹眰
1. [x] Entities.kt锛氭柊澧?FloorPlanEntity锛汣abinetEntity 鍔?floorPlanId/x/y 瀛楁
2. [x] Daos.kt锛欶loorPlanDao锛坕nsert/delete/rename/observeAll锛夛紱CabinetDao 鍔?observeByFloorPlan/updateFloorPlan/clearFloorPlan
3. [x] HomeStorageDatabase锛歷ersion+1锛孧igration锛堝姞琛?+ 3 鍒楋級
4. [x] HomeRepository锛氳浆鍙戞柟娉?5. [x] 楠岃瘉锛歚gradle assembleDebug` + 鍗曟祴锛堣縼绉绘祴璇曪級

### 闃舵 2锛氭埛鍨嬪浘鍒楄〃
6. [x] FloorPlanListScreen + FloorPlanListViewModel锛氬垪琛?鏂板锛圛magePicker+鍛藉悕寮圭獥锛?鍒犻櫎锛堢‘璁ゅ脊绐楋紝鎻愮ず"鏌滃瓙涓嶄細鍒犻櫎"锛?閲嶅懡鍚?7. [x] 棣栭〉椤堕儴"鎴峰瀷鍥?鍏ュ彛鎸夐挳 + 瀵艰埅
8. [x] 楠岃瘉锛氭瀯寤洪€氳繃

### 闃舵 3锛氭埛鍨嬪浘璇︽儏锛堟牳蹇冧氦浜掞級
9. [x] FloorPlanDetailScreen + ViewModel锛氬浘鐗?+ 鏌滃瓙鏍囪灞?+ 鎵嬪娍瑕嗙洊灞?10. [x] 鐐瑰嚮绌虹櫧 鈫?ModalBottomSheet锛堟柊寤烘煖瀛?/ 鎸傝浇宸叉湁鏌滃瓙锛?11. [x] 鏂板缓鏌滃瓙锛氳烦 CreateCabinetScreen锛屾垚鍔熷悗鍥炲～鍧愭爣鎸傝浇
12. [x] 鎸傝浇宸叉湁鏌滃瓙锛氬垪琛ㄥ脊绐楅€夋嫨
13. [x] 鐐瑰嚮鏍囪 鈫?璺宠浆鏌滃瓙璇︽儏锛涢暱鎸夋嫋鍔ㄧЩ鍔ㄤ綅缃?14. [x] 楠岃瘉锛氭瀯寤洪€氳繃 + 閫昏緫鑷煡

### 闃舵 4锛氭敹灏?15. [x] 鐪熸満楠岃瘉娓呭崟锛堝緟璁惧锛夛細涓婁紶鎴峰瀷鍥?鈫?鏂板缓鏌滃瓙鎸傝浇 鈫?绉诲姩鏍囪 鈫?璺宠浆璇︽儏
16. [x] trellis-check 璐ㄩ噺璧版煡
17. [x] 鍚堝苟 dev 鈫?main锛屽彂 v0.2.0

## 楠岃瘉鍛戒护

- 鏋勫缓锛歚$env:JAVA_HOME="D:\dev-envirment\jdk17"; $env:GRADLE_USER_HOME="D:\gradle-home"; & "D:\gradle-home\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat" assembleDebug`
- 娴嬭瘯锛氬悓涓婂姞 `testDebugUnitTest`

## 椋庨櫓/鍥炴粴鐐?
- `HomeStorageDatabase.kt`锛氳縼绉昏剼鏈纭€ф槸鏈€楂橀闄╋紝鍏堝姞杩佺Щ娴嬭瘯
- `FloorPlanDetailScreen.kt`锛氭墜鍔?鐐瑰嚮浜や簰澶嶇敤宸查獙璇佹ā寮忥紝鏃犳柊椋庨櫓
- 姣忛樁娈靛彲鐙珛鍥炴粴

