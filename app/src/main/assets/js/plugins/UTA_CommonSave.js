StorageManager.loadFromWebStorageCommonSave = function () {
  return AndroidBridge.loadGameData('common.rpgsave')
}

StorageManager.saveToWebStorageCommonSave = function (json) {
  AndroidBridge.saveGameData(json, 'common.rpgsave')
}

StorageManager.webStorageExistsCommonSave = function () {
  return AndroidBridge.existsGameSave('common.rpgsave')
}

StorageManager.removeWebStorageCommonSave = function () {
  AndroidBridge.removeCommonSave()
}
