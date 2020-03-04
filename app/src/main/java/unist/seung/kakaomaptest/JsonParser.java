package unist.seung.kakaomaptest;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class JsonParser {

    String address, roadName;

    void parse(JsonObject json) {
        JsonElement availability = json.getAsJsonObject("meta").get("total_count");
        String noRoadName = "도로명주소를 식별할 수 없습니다.";
        if (availability.toString().equals("1")) {
            JsonElement jsonRoadAddress = json.getAsJsonArray("documents").get(0).getAsJsonObject().get("road_address");

            if (!jsonRoadAddress.isJsonNull()) {
                address = jsonRoadAddress.getAsJsonObject().get("address_name").getAsString();
                roadName = jsonRoadAddress.getAsJsonObject().get("road_name").getAsString();

                Log.d("json", address + "");
            } else {
                JsonElement jsonAddress = json.getAsJsonArray("documents").get(0).getAsJsonObject().get("address");
                address = jsonAddress.getAsJsonObject().get("address_name").getAsString();
                roadName = noRoadName;
            }
        } else {
            address = "주소를 확인할 수 없습니다.";
            roadName = noRoadName;
        }
    }
}
