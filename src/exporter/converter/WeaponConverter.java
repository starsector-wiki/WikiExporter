package exporter.converter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exporter.model.Weapon;
import exporter.utils.JsonUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponConverter {
    private final ShipAPI playerShip;
    private final Map<String, JSONObject> csvObjectMap = new HashMap<>();
    private static final Map<String, String> projIdFileMap = new HashMap<>();

    static {
        projIdFileMap.put("locust", "locust_srm");
    }

    public WeaponConverter() throws JSONException, IOException {
        this.playerShip = Global.getCombatEngine().getPlayerShip();

        JSONArray csvArray = Global.getSettings().loadCSV("data/weapons/weapon_data.csv", true);
        for (int i = 0; i < csvArray.length(); i++) {
            JSONObject csvObject = csvArray.getJSONObject(i);
            String id = csvObject.getString("id");
            if (id != null && !id.isEmpty()) {
                this.csvObjectMap.put(id, csvObject);
            }
        }
    }

    public Weapon convert(WeaponSpecAPI weaponSpecAPI) throws JSONException, IOException {
        SettingsAPI settings = Global.getSettings();
        WeaponAPI.DerivedWeaponStatsAPI derivedStats = weaponSpecAPI.getDerivedStats();
        String id = weaponSpecAPI.getWeaponId();
        JSONObject csvObject = csvObjectMap.get(id);
        Description description = settings.getDescription(id, Description.Type.WEAPON);
        WeaponAPI fakeWeapon = Global.getCombatEngine().createFakeWeapon(playerShip, id);
        JSONObject jsonObject = settings.loadJSON("data/weapons/" + id + ".wpn", true);

        Weapon weapon = new Weapon();
        weapon.setId(id);
        weapon.setJsonType("WEAPON");
        weapon.setName(weaponSpecAPI.getWeaponName());
        weapon.setDescription("");
        if (description.hasText1()) {
            weapon.setDescription(description.getText1());
        }
        if (description.hasText2()) {
            weapon.setDescription(weapon.getDescription() + "\n" + description.getText2());
        }
        weapon.setCustomPrimary(weaponSpecAPI.getCustomPrimary());
        weapon.setTurretSprite(weaponSpecAPI.getTurretSpriteName());
        weapon.setTurretUnderSprite(weaponSpecAPI.getTurretUnderSpriteName());
        weapon.setTurretGunSprite(JsonUtils.getString(jsonObject, "turretGunSprite"));
        weapon.setTurretGlowSprite(JsonUtils.getString(jsonObject, "turretGlowSprite"));
        weapon.setHardPointSprite(weaponSpecAPI.getHardpointSpriteName());
        weapon.setHardPointUnderSprite(weaponSpecAPI.getHardpointUnderSpriteName());
        weapon.setHardPointGunSprite(JsonUtils.getString(jsonObject, "hardpointGunSprite"));
        weapon.setHardPointGlowSprite(JsonUtils.getString(jsonObject, "hardpointGlowSprite"));
        List<String> renderHints = new ArrayList<>();
        if (jsonObject.has("renderHints")) {
            JSONArray renderHintsArray = jsonObject.getJSONArray("renderHints");
            for (int i = 0; i < renderHintsArray.length(); i++) {
                renderHints.add(renderHintsArray.getString(i));
            }
        }
        weapon.setRenderHints(renderHints);
        List<Integer> offsets = new ArrayList<>();
        if (jsonObject.has("turretOffsets")) {
            JSONArray renderHintsArray = jsonObject.getJSONArray("turretOffsets");
            for (int i = 0; i < renderHintsArray.length(); i++) {
                offsets.add(renderHintsArray.getInt(i));
            }
        }
        weapon.setTurretOffsets(offsets);
        List<Integer> hardPointoffsets = new ArrayList<>();
        if (jsonObject.has("hardpointOffsets")) {
            JSONArray renderHintsArray = jsonObject.getJSONArray("hardpointOffsets");
            for (int i = 0; i < renderHintsArray.length(); i++) {
                hardPointoffsets.add(renderHintsArray.getInt(i));
            }
        }
        weapon.setHardPointOffsets(hardPointoffsets);
        Object projectileSpec = weaponSpecAPI.getProjectileSpec();
        if (projectileSpec instanceof ProjectileSpecAPI) {
            ProjectileSpecAPI projectileSpecAPI = (ProjectileSpecAPI) projectileSpec;
            weapon.setProjSpriteName(projectileSpecAPI.getBulletSpriteName());
        } else {
            String projId = JsonUtils.getString(jsonObject, "projectileSpecId");
            if (projId != null) {
                String projFile = projIdFileMap.get(projId);
                if (projFile == null) {
                    projFile = projId;
                }
                JSONObject projJson = settings.loadJSON("data/weapons/proj/" + projFile + ".proj", true);
                weapon.setProjSpriteName(JsonUtils.getString(projJson, "sprite"));
            }
        }

        weapon.setPrimaryRoleStr(weaponSpecAPI.getPrimaryRoleStr());
        weapon.setSize(weaponSpecAPI.getSize().name());
        weapon.setMountType(weaponSpecAPI.getMountType().name());
        weapon.setOrdnancePoint(JsonUtils.getDouble(csvObject, "OPs", 0));

        weapon.setMaxRange(weaponSpecAPI.getMaxRange());
        weapon.setDamagePerShot(derivedStats.getDamagePerShot());
        weapon.setBurstSize(weaponSpecAPI.getBurstSize());
        weapon.setDps(derivedStats.getDps());
        weapon.setSustainedDps(derivedStats.getSustainedDps());
        weapon.setEmpPerShot(derivedStats.getEmpPerShot());
        weapon.setEmpPerSecond(derivedStats.getEmpPerSecond());

        weapon.setFluxPerSecond(derivedStats.getFluxPerSecond());
        weapon.setSustainedFluxPerSecond(derivedStats.getSustainedFluxPerSecond());
        weapon.setFluxPerDamage(derivedStats.getFluxPerDam());

        weapon.setCustomAncillary(weaponSpecAPI.getCustomAncillary());
        weapon.setDamageType(weaponSpecAPI.getDamageType());
        weapon.setSpeedStr(weaponSpecAPI.getSpeedStr());
        weapon.setTrackingStr(weaponSpecAPI.getTrackingStr());
        weapon.setAccuracyStr(weaponSpecAPI.getAccuracyStr());
        weapon.setTurnRateStr(weaponSpecAPI.getTurnRateStr());
        weapon.setMaxAmmo(weaponSpecAPI.getMaxAmmo() == Integer.MAX_VALUE ? null : weaponSpecAPI.getMaxAmmo());
        float ammoPerSec = weaponSpecAPI.getAmmoPerSecond();
        int reloadSize = weaponSpecAPI.getReloadSize() >= 1 ? Math.round(weaponSpecAPI.getReloadSize()) : 1;
        weapon.setReloadTime(ammoPerSec == 0 ? null : Math.round(reloadSize / ammoPerSec));
        weapon.setReloadSize(reloadSize);
        weapon.setReFireDelay(fakeWeapon.getRefireDelay());

        weapon.setTags(weaponSpecAPI.getTags());

        return weapon;
    }
}
