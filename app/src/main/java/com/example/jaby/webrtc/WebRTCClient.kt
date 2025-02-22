package com.example.jaby.webrtc

import android.content.Context
import android.util.Log
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.google.gson.Gson
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCClient @Inject constructor(
    private val context: Context,
    private val gson:Gson
) {
    //class variables
    var listener: Listener?=null
    private lateinit var deviceName: String

    //webrtc variables
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy {createPeerConnectionFactory()}
    private var peerConnection:PeerConnection?=null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer(),
//        PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80")
//            .createIceServer()
//        ,
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
            .setUsername("fe969608e247adcf08a4f7b9")
            .setPassword("v5QRnPJXseRHGVUY")
            .createIceServer()
        ,
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
            .setUsername("fe969608e247adcf08a4f7b9")
            .setPassword("v5QRnPJXseRHGVUY")
            .createIceServer()
        ,
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
            .setUsername("fe969608e247adcf08a4f7b9")
            .setPassword("v5QRnPJXseRHGVUY")
            .createIceServer()
        ,
        PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
            .setUsername("fe969608e247adcf08a4f7b9")
            .setPassword("v5QRnPJXseRHGVUY")
            .createIceServer()
    )
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false)}
    private val localAudioSource by lazy {peerConnectionFactory.createAudioSource(MediaConstraints())}
    private val videoCapturer = getVideoCapturer(context)
    private var surfaceTextureHelper:SurfaceTextureHelper?=null
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }


    //call variables
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var localStream: MediaStream?=null
    private var localTrackId = ""
    private var localStreamId = ""
    private var localAudioTrack:AudioTrack?=null
    private var localVideoTrack:VideoTrack?=null


    //installing requirements section
    init {
        initPeerConnectionFactory()
    }
    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }
    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBaseContext)
            ).setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBaseContext,true,true
                )
            ).setOptions(PeerConnectionFactory.Options().apply{
                disableNetworkMonitor = false
                disableEncryption = false
            }).createPeerConnectionFactory()
    }

    fun initializeWebrtcClient(deviceName: String, observer:PeerConnection.Observer) {
        this.deviceName = deviceName
        localTrackId = "${deviceName}_track"
        localStreamId = "${deviceName}_stream"
        peerConnection = createPeerConnection(observer)
    }
    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL
        return peerConnectionFactory.createPeerConnection(rtcConfig, observer)
    }

    //negotiation section
    fun call(target:String) {
        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(DataModel(type = DataModelType.Offer,
                            sender = deviceName,
                            target = target,
                            data = desc?.description))
                    }
                },desc)
            }
        },mediaConstraint)
    }

    fun answer(target:String) {
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            DataModel(type = DataModelType.Answer,
                                sender = deviceName,
                                target = target,
                                data = desc?.description)
                        )
                    }
                },desc)
            }
        },mediaConstraint)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(MySdpObserver(),sessionDescription)
    }
    fun addIceCandidateToPeer(iceCandidate: IceCandidate){
        peerConnection?.addIceCandidate(iceCandidate)
    }
    fun sendIceCandidate(target: String, iceCandidate: IceCandidate) {
        addIceCandidateToPeer(iceCandidate)
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.IceCandidates,
                sender = deviceName,
                target = target,
                data = gson.toJson(iceCandidate)
            )
        )
    }
    fun closeConnection() {
        try {
            videoCapturer.dispose()
            localStream?.dispose()
            peerConnection?.close()
        }catch (e:Exception) {
            e.printStackTrace()
        }
    }
    fun switchCamera(){
        videoCapturer.switchCamera(null)
    }
    fun toggleAudio(shouldBeMuted: Boolean) {
        if(shouldBeMuted) {
            localStream?.removeTrack(localAudioTrack)
        } else {
            localStream?.addTrack(localAudioTrack)
        }
    }
    fun toggleVideo(shouldBeMuted: Boolean) {
        try {
            if(shouldBeMuted){
                stopCapturingCamera()
            } else {
                startCapturingCamera(localSurfaceView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //streaming section
    private fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext,null)
        }
    }
    fun initRemoteSurfaceView(view:SurfaceViewRenderer) {
        this.remoteSurfaceView = view
        initSurfaceView(view)
    }
    fun initLocalSurfaceView(localView: SurfaceViewRenderer,isMonitor: Boolean) {
        this.localSurfaceView = localView
        initSurfaceView(localView)
        startLocalStreaming(localView,isMonitor)
    }
    private fun startLocalStreaming(localView: SurfaceViewRenderer, isMonitor: Boolean) {
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        if(isMonitor) {
//            startCapturingCamera(localView)
        }
        startCapturingCamera(localView)
        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId + "_audio", localAudioSource)
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }
    private fun startCapturingCamera(localView: SurfaceViewRenderer) {
        surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,eglBaseContext
        )

        videoCapturer.initialize(
            surfaceTextureHelper,context,localVideoSource.capturerObserver
        )

        videoCapturer.startCapture(
            720,480,20
        )

        localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId + "_video",localVideoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
    }
    private fun getVideoCapturer(context:Context):CameraVideoCapturer =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    private fun stopCapturingCamera() {
        videoCapturer.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localVideoTrack)
        localVideoTrack?.dispose()
    }


    interface Listener {
        fun onTransferEventToSocket(data:DataModel)
    }
}