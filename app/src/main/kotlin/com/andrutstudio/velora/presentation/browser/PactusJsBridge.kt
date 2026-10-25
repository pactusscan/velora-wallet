package com.andrutstudio.velora.presentation.browser

import android.webkit.JavascriptInterface

class PactusJsBridge(
    private val onGetAccounts: () -> String,
    private val onGetNetwork: () -> String,
    private val onConnectRequest: (requestId: String) -> Unit,
    private val onSignRequest: (requestId: String, to: String, amountNanoPac: String, feeNanoPac: String, memo: String) -> Unit,
    private val onSignMessageRequest: (requestId: String, message: String) -> Unit,
) {
    @JavascriptInterface
    fun getAccounts(): String = onGetAccounts()

    @JavascriptInterface
    fun getNetwork(): String = onGetNetwork()

    @JavascriptInterface
    fun requestConnect(requestId: String) {
        onConnectRequest(requestId)
    }

    @JavascriptInterface
    fun requestSign(requestId: String, to: String, amountNanoPac: String, feeNanoPac: String, memo: String) {
        onSignRequest(requestId, to, amountNanoPac, feeNanoPac, memo)
    }

    @JavascriptInterface
    fun requestSignMessage(requestId: String, message: String) {
        onSignMessageRequest(requestId, message)
    }

    companion object {
        /**
         * Official Velora logo shown in dApp wallet pickers. Minified from
         * velora-icon.svg and embedded as a plain-text data URI (only '#' is
         * percent-encoded), which is ~35% smaller than base64.
         */
        private const val ICON_DATA_URI =
            "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'><defs><linearGradient id='g' x1='2.32' y1='.74' x2='5.69' y2='5.76' gradientTransform='translate(-140.6 -63.84) scale(100)' gradientUnits='userSpaceOnUse'><stop offset='0' stop-color='%2311b9a5'/><stop offset='1' stop-color='%236e6cf2'/></linearGradient></defs><rect width='512' height='512' rx='153.61' fill='url(%23g)'/><path fill='%23fff' fill-rule='evenodd' d='M334.28,178.24s-.09-.02-.12-.03l-40.55-14.91-36.18-13.3c-1.07-.4-1.72-.4-2.78,0l-36.18,13.3-40.55,14.91s-.09.02-.12.03c-3.76,1.29-8.28,3.85-8.66,10.15-.65,10.84-.69,21.73-.69,32.59,0,6.49.12,13.01.43,19.47h0c.26,5.77.67,11.51,1.2,17.23.27,2.94.6,5.86.96,8.81.6,4.85,4.66,8.42,9.54,8.42h6.31c2.87,0,5.33-1.12,7.22-3.26,1.89-2.13,2.68-4.74,2.32-7.56-.5-4.04-.93-8.08-1.27-12.11-.34-4.11-.62-8.25-.81-12.37-.27-6.19-.43-12.41-.43-18.61,0-5.62,0-11.22.1-16.86.07-2.92,1.82-5.38,4.55-6.39l55.05-20.24c1.62-.58,3.18-.58,4.79,0l53.38,19.62s.12.03.21.09c9.97,4.09,17.01,6.61,22.85,9.86,1.86,1.03,3.97.98,5.81-.1,1.82-1.12,2.83-2.96,2.8-5.09-.1-4.66-.26-9.35-.5-13.49-.38-6.32-4.88-8.87-8.66-10.15h0l-.02-.02Z M384.11,166.47c-.14-8.88-5.46-16.37-13.78-19.43l-107.03-39.35c-2.44-.91-4.86-1.36-7.29-1.36s-4.81.45-7.29,1.36l-107.03,39.35c-8.35,3.06-13.64,10.55-13.78,19.43-.58,35.7-3.73,71.25,3.11,115.24.14.93.31,1.7.57,2.58,4,13.95,10.26,26.89,17.95,38.76,1.41,2.18,4,3.14,6.51,2.41,2.49-.74,4.16-2.94,4.16-5.57v-29.81c0-.79-.12-1.44-.43-2.18-1.17-2.92-2.23-5.89-3.16-8.88-.31-1.01-.5-1.89-.67-2.94-2.75-18.32-3.76-36.56-3.76-55.12,0-12.47.45-24.93.82-37.39.24-8.45,5.34-15.5,13.26-18.4l82.76-30.41c2.34-.86,4.67-1.29,6.99-1.29s4.64.43,6.99,1.29l82.76,30.41c7.92,2.92,13.01,9.98,13.26,18.4.36,12.46.81,24.93.82,37.39,0,18.54-1.03,36.77-3.76,55.12-.15,1.05-.36,1.94-.67,2.94-7.61,24.88-24.31,46.46-43.38,63.76-13.47,12.22-30,23.93-46.79,32.54-2.99,1.53-6.1,2.3-9.23,2.3s-6.25-.77-9.23-2.3c-6.65-3.4-13.26-7.3-19.66-11.51-1.84-1.2-4-1.31-5.94-.26-1.94,1.03-3.04,2.92-3.04,5.1v16.41c0,2.08,1.01,3.87,2.78,4.97,9.71,5.91,18.93,10.57,26.86,13.93,2.71,1.15,5.48,1.74,8.23,1.74s5.5-.58,8.23-1.74c36.03-15.27,98.37-57.52,116.2-119.62.26-.89.43-1.67.57-2.58,6.82-43.99,3.69-79.53,3.11-115.24h-.07v-.05Z M314.85,268.91c-5.69,16.55-18.08,31.41-30.82,42.97-7.44,6.75-15.64,13.04-24.28,18.54-2.34,1.49-5.14,1.49-7.47,0-1.98-1.27-3.93-2.56-5.86-3.92-1.01-.7-2.08-1.03-3.3-1.03h-19.17c-3.18,0-5.79,2.59-5.79,5.79v3.01c0,1.82.74,3.38,2.18,4.52,10.52,8.42,22.1,16.05,33.87,22.08.67.34,1.25.5,1.84.5s1.17-.15,1.84-.5c15.41-7.89,30.48-18.5,43.28-30.12,16.84-15.26,32.03-34.52,38.76-56.48.07-.21.1-.36.14-.58,2.18-14.64,3.23-29.33,3.51-44.11.02-1.32-.34-2.49-1.15-3.57-1.24-1.65-2.58-3.21-4.09-4.67-3.18-3.11-7.1-5.84-11.87-8.14-1.86-.89-3.85-.77-5.58.33-1.74,1.08-2.7,2.87-2.7,4.9v2.56c0,15.58-.81,31.18-2.96,46.63-.07.46-.15.84-.31,1.29h-.03l-.02.02Z M260.59,308.18c14.21-11.01,27.16-23.37,36.36-39.81,2.03-3.63,4.19-6.65,4.19-10.81v-46.91c0-3.68-1.98-5.33-5.45-6.36-2.71-.81-2.87-1.31-5.58-2.11-3.81-1.13-7.08,1.63-7.08,5.45l-.07,43.33c0,4.18-2.22,7.18-4.23,10.82-4.67,8.45-11.01,15.58-19.43,21.1-3.88,2.54-7.13,5.15-11.79,5.15-22.9,0-47.59-.1-70.08-.1-.24,0-.46,0-.7.02-4.9.36-8.81,4.5-8.81,9.47v81.44c0,10.21,8.35,18.54,18.54,18.54s18.23-8.02,18.52-17.95v-60.74c0-1.6.57-2.96,1.7-4.09s2.49-1.72,4.11-1.7l37.75.43c4.72.07,8.26-2.32,11.99-5.22h.03l.03.05Z M259.46,190.97c-2.32-.84-4.55-.84-6.87,0l-39.84,14.66c-3.99,1.48-6.51,5.09-6.53,9.33v6.05c0,34.81-3.87,53.92,7.11,53.92h28.68c12.53,0,26.65-16.55,26.65-29.55v-42.8c0-5.03-3.01-9.35-7.75-11.08l-1.43-.53h-.02v.02Z'/></svg>"

        /**
         * Injected into every page, ideally at document-start so dApps that
         * check for the provider during startup (e.g. play.pactusscan) find it.
         * Creates window.pactus (same shape as the Velora browser extension:
         * getAccounts / getNetwork / connect / signTransaction / signMessage),
         * fires `pactus#initialized`, and speaks the multi-wallet announce
         * protocol (`pactus:requestProvider` / `pactus:announceProvider`).
         * Safe to evaluate more than once: bails if window.pactus exists.
         */
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
                connect:function(){
                  return new Promise(function(res,rej){
                    var id='c'+Date.now().toString(36)+Math.random().toString(36).slice(2,6);
                    _cb[id]={resolve:res,reject:rej};
                    PactusAndroid.requestConnect(id);
                  });
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
                signMessage:function(message){
                  return new Promise(function(res,rej){
                    var id='m'+Date.now().toString(36)+Math.random().toString(36).slice(2,6);
                    _cb[id]={resolve:res,reject:rej};
                    PactusAndroid.requestSignMessage(id,message);
                  });
                },
                _resolve:function(id,r){if(_cb[id]){_cb[id].resolve(r);delete _cb[id];}},
                _resolveConnect:function(id){if(_cb[id]){_cb[id].resolve(_pactus.getAccounts());delete _cb[id];}},
                _reject:function(id,e){if(_cb[id]){_cb[id].reject(new Error(e));delete _cb[id];}}
              };
              Object.defineProperty(window,'pactus',{value:_pactus,writable:false,configurable:false});
              var _info={
                uuid:'velora-android-'+Math.random().toString(36).slice(2,10),
                name:'Velora Wallet',
                icon:"$ICON_DATA_URI"
              };
              function _announce(){
                try{
                  window.dispatchEvent(new CustomEvent('pactus:announceProvider',{detail:{info:_info,provider:_pactus}}));
                }catch(e){}
              }
              window.addEventListener('pactus:requestProvider',_announce);
              _announce();
              function _init(){
                try{document.dispatchEvent(new Event('pactus#initialized'));}catch(e){}
                try{window.dispatchEvent(new Event('pactus#initialized'));}catch(e){}
              }
              if(document.readyState==='loading'){
                document.addEventListener('DOMContentLoaded',_init);
              }
              _init();
            })();
        """.trimIndent()
    }
}
