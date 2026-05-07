package com.andrutstudio.velora.presentation.browser

import android.webkit.JavascriptInterface

class PactusJsBridge(
    private val onGetAccounts: () -> String,
    private val onGetNetwork: () -> String,
    private val onSignRequest: (requestId: String, to: String, amountNanoPac: String, feeNanoPac: String, memo: String) -> Unit,
) {
    @JavascriptInterface
    fun getAccounts(): String = onGetAccounts()

    @JavascriptInterface
    fun getNetwork(): String = onGetNetwork()

    @JavascriptInterface
    fun requestSign(requestId: String, to: String, amountNanoPac: String, feeNanoPac: String, memo: String) {
        onSignRequest(requestId, to, amountNanoPac, feeNanoPac, memo)
    }

    companion object {
        /** Injected into every page on load. Creates window.pactus. */
        val JS_BRIDGE = """
            (function(){
              if(window.pactus)return;
              var _cb={};
              var _pactus={
                getAccounts:function(){
                  try{return JSON.parse(PactusAndroid.getAccounts());}catch(e){return[];}
                },
                getNetwork:function(){
                  try{return PactusAndroid.getNetwork();}catch(e){return'mainnet';}
                },
                signTransaction:function(p){
                  return new Promise(function(res,rej){
                    var id='r'+Date.now().toString(36)+Math.random().toString(36).slice(2,6);
                    _cb[id]={resolve:res,reject:rej};
                    PactusAndroid.requestSign(
                      id,
                      p.to||'',
                      String(p.amountNanoPac||0),
                      String(p.feeNanoPac||0),
                      p.memo||''
                    );
                  });
                },
                _resolve:function(id,r){if(_cb[id]){_cb[id].resolve(r);delete _cb[id];}},
                _reject:function(id,e){if(_cb[id]){_cb[id].reject(new Error(e));delete _cb[id];}}
              };
              Object.defineProperty(window,'pactus',{value:_pactus,writable:false,configurable:false});
            })();
        """.trimIndent()
    }
}
