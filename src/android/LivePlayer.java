package xwang.cordova.vcloud.liveplayer;

import android.content.Intent;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class LivePlayer extends CordovaPlugin {
  private static final String ERROR_INVALID_PARAMETERS = "参数错误";

  @Override
  public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("play")) {
      return play(args, callbackContext);
    }
    else if (action.equals("channel")) {
      return channel(args, callbackContext);
    }
    else if (action.equals("message")) {
      return message(args, callbackContext);
    }
    return false;
  }

  protected boolean play(CordovaArgs args, final CallbackContext callbackContext) {
    final String url, title;
    try {
      url = args.getString(0);
      title = args.getString(1);
    } catch (JSONException e) {
      callbackContext.error(ERROR_INVALID_PARAMETERS);
      return true;
    }

    Intent intent = new Intent(this.cordova.getActivity().getApplicationContext(), LivePlayerActivity.class)
    .putExtra("url", url)
    .putExtra("title", title);
    this.cordova.getActivity().startActivity(intent);

    sendNoResultPluginResult(callbackContext, false);
    return true;
  }

  protected boolean channel(CordovaArgs args, final CallbackContext callbackContext) {
    final String name, message;
    try {
      name = args.getString(0);
      message = args.getString(1);
    } catch (JSONException e) {
      callbackContext.error(ERROR_INVALID_PARAMETERS);
      return true;
    }
    LivePlayerActivity.addChannelMessage(name, message);

    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
    callbackContext.sendPluginResult(result);
    return true;
  }

  protected boolean message(CordovaArgs args, final CallbackContext callbackContext) {
    LivePlayerActivity.setMessageCallbackContext(callbackContext);
    return true;
  }

  private void sendNoResultPluginResult(CallbackContext callbackContext, boolean keep) {
    PluginResult result = new PluginResult(PluginResult.Status.OK);
    result.setKeepCallback(keep);
    callbackContext.sendPluginResult(result);
  }
}