package com.kapodamy.twilighttweaker;

/**
 * Created by kapodamy on 11/03/2018.
 */

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;


public class XposedMain implements IXposedHookLoadPackage {
    public static final int VERSION = 2;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        final XSharedPreferences ajustes = new XSharedPreferences(XposedMain.class.getPackage().getName(), "ajustes");
        ajustes.reload();// ¿es necesario?

        if (ajustes.getAll().size() < 1) {
            return;
        }

        if (ajustes.getBoolean("desactivar", true)) {
            return;
        }

        Set<String> lista_apk = ajustes.getStringSet("lista", new HashSet<String>());
        boolean admision = ajustes.getBoolean("admision", true);

        // por defecto la aplicación misma siempre esta en la lista blanca
        if (lista_apk.size() > 0 && !lpparam.packageName.equals(XposedMain.class.getPackage().getName())) {
            boolean flag = lista_apk.contains(lpparam.packageName);

            if (admision && !flag) {
                return;// no se encuentra en la lista blanca
            } else if (!admision && flag) {
                return;// en lista negra ignorar
            }
        }

        // hook de la aplicación en carga
        Class<?> clase = null;

        try {
            clase = findClass("android.support.v7.app.TwilightManager", lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError err) {
            return;// La aplicación no usa la libreria de soporte
        }

        findAndHookMethod(clase, "isNight", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ajustes.reload();

                if (ajustes.getBoolean("desactivar", true)) {
                    return;// desactivado, invocar metodo original
                }

                int modo = ajustes.getInt("modo", 0);// 0: reloj   1: forzar dia   2: forzar noche
                if (modo > 0) {
                    param.setResult(modo == 2); // forzar a una jornada especifica
                    return;
                }

                boolean usar_inicio = ajustes.getBoolean("usar_inicio", false);
                boolean usar_final = ajustes.getBoolean("usar_final", false);

                if (!usar_inicio && !usar_final) {
                    return;// sin horario manual, invocar metodo original
                }

                int minutos_final = ajustes.getInt("minutos_final", 360);
                int minutos_inicio = ajustes.getInt("minutos_inicio", 1140);

                if (minutos_inicio == minutos_final) {
                    XposedBridge.log(XposedMain.class.getPackage().getName().concat(": ERROR los horarios son iguales!"));
                    return;// invocar metodo original
                }

                // verificar horario
                Calendar calendario = Calendar.getInstance();
                int ts_actual = (calendario.get(Calendar.HOUR_OF_DAY) * 60) + calendario.get(Calendar.MINUTE);

                if (usar_inicio != usar_final) {
                    if (usar_inicio && ts_actual >= minutos_inicio) {
                        param.setResult(true);
                        return;
                    }

                    if (usar_final && ts_actual <= minutos_final) {
                        param.setResult(false);
                        return;
                    }

                    // ninguna de las condiciones se cumple, invocar metodo original
                    return;
                }

                /** Calculo de la jornada nocturna:
                 *  -> empieza si la hora actual es igual o mayor a {minutos_incio}.
                 *  -> finaliza antes de {minutos_final}
                 */
                param.setResult(!(ts_actual >= minutos_final && ts_actual < minutos_inicio));
            }
        });
    }
}

