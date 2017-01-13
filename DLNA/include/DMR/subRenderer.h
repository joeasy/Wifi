#ifndef __DMR_SUBRENDER__
#define __DMR_SUBRENDER__

#include <OSAL.h>
#define S_OK 0
#define E_FAIL -1
typedef enum 
{
	DMR_SUBR_NOTHING    = 0x0,
	DMR_SUBR_AUDIO      = 0x1,
	DMR_SUBR_VIDEO      = 0x2,
	DMR_SUBR_IMAGE      = 0x4,
	DMR_SUBR_PLAYLIST   = 0x8,
	DMR_SUBR_TEXT       = 0x10,
	DMR_SUBR_UNKNOWN    = 0x20,
}SUBRENDERER_TYPE;

class subRenderer {
public:
	SUBRENDERER_TYPE m_subRenderer_type;
	virtual ~subRenderer() {};

	virtual int preParse(char *filename, unsigned int *NumberOfTracks, char ***MediaTrackURI, SUBRENDERER_TYPE **MediaType, long *TotalTime,char ***ProtocalInfo = NULL) = 0;
	virtual int loadMedia(char *filename) = 0;
	virtual int Play(char *filename, int speed) = 0;
	virtual void Stop() = 0;
	virtual int Pause(bool Pause=true)= 0;
	virtual int SeekMediaPosition(int titleNum, long position){return E_FAIL;};
	virtual int SetRate(int rate){return E_FAIL;};
	virtual int GetResolution(int*x,int*y){return E_FAIL;};
	virtual void SetProtocolInfo(char *pinfo){}; 
	SUBRENDERER_TYPE GetRendererType(){ return m_subRenderer_type;};
};

#endif // __DMR_SUBRENDER__

