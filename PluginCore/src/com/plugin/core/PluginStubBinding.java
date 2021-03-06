package com.plugin.core;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.plugin.core.stub.ui.PluginStubActivity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * LaunchMode动态绑定，解决singleTask问题。
 */
public class PluginStubBinding {

	public static final String STUB_ACTIVITY_PRE = PluginStubActivity.class.getPackage().getName();

	private static final String ACTION_LAUNCH_MODE = "com.plugin.core.LAUNCH_MODE";

	/**
	 * key:stub Activity Name
	 * value:plugin Activity Name
	 */
	private static HashMap<String, String> singleTaskMapping = new HashMap<String, String>();
	private static HashMap<String, String> singleTopMapping = new HashMap<String, String>();
	private static HashMap<String, String> singleInstanceMapping = new HashMap<String, String>();

	private static boolean isPoolInited = false;

	public static String bindLaunchModeStubActivity(String pluginActivityClassName, int launchMode) {

		initPool();

		String stubActivityName = null;

		Iterator<Map.Entry<String, String>> itr = null;

		if (launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {

			itr = singleTaskMapping.entrySet().iterator();

		} else if (launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {

			itr = singleTopMapping.entrySet().iterator();

		} else if (launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {

			itr = singleInstanceMapping.entrySet().iterator();

		}

		if (itr != null) {

			String idleStubActivityName = null;

			while (itr.hasNext()) {
				Map.Entry<String, String> entry = itr.next();
				if (entry.getValue() == null) {
					if (idleStubActivityName == null) {
						idleStubActivityName = entry.getKey();
					}
				} else if (pluginActivityClassName.equals(entry.getValue())) {
					return entry.getKey();
				}
			}

			//没有绑定到StubActivity，而且还有空余的stubActivity，进行绑定
			if (idleStubActivityName != null) {
				singleTaskMapping.put(idleStubActivityName, pluginActivityClassName);
				return idleStubActivityName;
			}

		}

		//绑定失败
		return PluginStubActivity.class.getName();
	}

	private static void initPool() {
		if (isPoolInited) {
			return;
		}

		Intent launchModeIntent = new Intent();
		launchModeIntent.setAction(ACTION_LAUNCH_MODE);
		launchModeIntent.setPackage(PluginLoader.getApplicatoin().getPackageName());

		List<ResolveInfo> list = PluginLoader.getApplicatoin().getPackageManager().queryIntentActivities(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

		if (list != null && list.size() >0) {
			for (ResolveInfo resolveInfo:
					list) {
				if (resolveInfo.activityInfo.name.startsWith(STUB_ACTIVITY_PRE)) {

					if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {

						singleTaskMapping.put(resolveInfo.activityInfo.name, null);

					} else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {

						singleTopMapping.put(resolveInfo.activityInfo.name, null);

					} else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {

						singleInstanceMapping.put(resolveInfo.activityInfo.name, null);

					}

				}
			}
		}

		isPoolInited = true;
	}

	public static void unBindLaunchModeStubActivity(String activityName, Intent intent) {
		if (activityName.startsWith(PluginStubBinding.STUB_ACTIVITY_PRE)) {
			if (intent != null) {
				ComponentName cn = intent.getComponent();
				if (cn != null) {
					String pluginActivityName = cn.getClassName();
					if (pluginActivityName.equals(singleTaskMapping.get(activityName))) {
						singleTaskMapping.put(activityName, null);
					} else if (pluginActivityName.equals(singleInstanceMapping.get(activityName))) {
						singleInstanceMapping.put(activityName, null);
					} else {
						//对于standard和singleTop的launchmode，不做处理。
					}
				}
			}
		}
	}
}
