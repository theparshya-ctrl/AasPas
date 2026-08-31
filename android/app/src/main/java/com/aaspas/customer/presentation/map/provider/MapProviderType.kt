package com.aaspas.customer.presentation.map.provider

enum class MapProviderType {
    Google,
    OpenStreetMap,
}

enum class MapRenderState {
    Loading,
    Ready,
    Unavailable,
}
