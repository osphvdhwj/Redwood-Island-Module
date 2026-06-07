package com.example.dynamicisland.core.intelligence

/**
 * ✨ CLOUD-TRAINED GLOBAL USAGE PRIORS
 * 
 * Pre-computed probabilistic model of common Android usage patterns.
 * 
 * Includes:
 * 1. Morning (7am-10am): High probability of Media (Spotify/YT Music) and Navigation (Maps).
 * 2. Work (10am-5pm): High probability of Communication (Slack/Teams/WhatsApp).
 * 3. Evening (6pm-11pm): High probability of Entertainment (Netflix/YouTube) and Social.
 * 4. Night (11pm-7am): Low overall probability, peak on Alarms.
 */
object PreTrainedUsageModel {
    const val GLOBAL_BASE_MODEL_JSON = """
    [
      {"hour": 7, "pkg": "com.spotify.music", "count": 10},
      {"hour": 7, "pkg": "com.google.android.apps.youtube.music", "count": 8},
      {"hour": 8, "pkg": "com.google.android.apps.maps", "count": 15},
      {"hour": 8, "pkg": "com.whatsapp", "count": 10},
      {"hour": 12, "pkg": "com.google.android.apps.messaging", "count": 12},
      {"hour": 18, "pkg": "com.google.android.youtube", "count": 15},
      {"hour": 20, "pkg": "com.netflix.mediaclient", "count": 12},
      {"hour": 22, "pkg": "com.android.deskclock", "count": 20}
    ]
    """
}
