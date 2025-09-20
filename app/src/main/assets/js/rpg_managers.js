StorageManager.saveToWebStorage = function (savefileId, json) {
  const name =
    savefileId < 0
      ? 'config.rpgsave'
      : savefileId === 0
      ? 'global.rpgsave'
      : `file${savefileId}.rpgsave`

  AndroidBridge.saveGameData(json, name)
}

StorageManager.loadFromWebStorage = function (savefileId) {
  const name =
    savefileId < 0
      ? 'config.rpgsave'
      : savefileId === 0
      ? 'global.rpgsave'
      : `file${savefileId}.rpgsave`

  return AndroidBridge.loadGameData(name)
}

StorageManager.webStorageExists = function (savefileId) {
  const name =
    savefileId < 0
      ? 'config.rpgsave'
      : savefileId === 0
      ? 'global.rpgsave'
      : `file${savefileId}.rpgsave`

  return AndroidBridge.existsGameSave(name)
}
