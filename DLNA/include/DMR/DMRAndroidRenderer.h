#ifndef __DMR_ANDROID_RENDER__
#define __DMR_ANDROID_RENDER__

#include <subRenderer.h>

class AndroidRenderer : public subRenderer{
private:
	char *m_protocolInfo;
public:
	AndroidRenderer(SUBRENDERER_TYPE renderType);
	~AndroidRenderer();
        static char* uuid;
	static int status;
        static bool force_stop_thread;
	int preParse(char *filename, unsigned int *NumberOfTracks, char ***MediaTrackURI, SUBRENDERER_TYPE **MediaType, long *TotalTime,char ***ProtocalInfo = NULL);
	int loadMedia(char *filename);
	int Play(char *filename, int speed);
	void Stop();
	int Pause(bool Pause=true);
	static int Restart(int Restart, void* start);
	static int QueryForConnection(int Query, void* query);
	static int SetBrightness(int Bright , void* bright);
	static int SetContrast(int Contrast, void * contrast);
	static int ShowVolumeStatus(int Volume, void* volume);
	static int SetMute(int Mute, void* mute);
        int SeekMediaPosition(int titleNum, long position);
	static int setSeekPosition(int position);
	static int GetUUID();
	static int Init();
	void SetProtocolInfo(char *pinfo);
        static void* PlayMonitor(void *);
	static void DMRAttachCurrentThread(void** env);
	static void DMRDetachCurrentThread();
	int SetRate(int rate);
};

#endif // __DMR_ANDROID_RENDER__

