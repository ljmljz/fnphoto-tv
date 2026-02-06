package com.fnphoto.tv.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FnModels {
    // Auth request
    public static class LoginRequest {
        public String username;
        public String password;
        public LoginRequest(String u, String p) { this.username = u; this.password = p; }
    }

    // Auth response
    public static class LoginResponse {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("token_type")
        public String tokenType;
    }

    // Album response
    public static class AlbumResponse {
        public List<Album> data;
    }

    public static class Album {
        public String id;
        public String name;
        @SerializedName("photo_count")
        public int photoCount;
        @SerializedName("cover_id")
        public String coverId;
    }
}
