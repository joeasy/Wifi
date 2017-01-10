/*
 * INTEL CONFIDENTIAL
 * Copyright (c) 2002 - 2005 Intel Corporation.  All rights reserved.
 * 
 * The source code contained or described herein and all documents
 * related to the source code ("Material") are owned by Intel
 * Corporation or its suppliers or licensors.  Title to the
 * Material remains with Intel Corporation or its suppliers and
 * licensors.  The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and
 * licensors. The Material is protected by worldwide copyright and
 * trade secret laws and treaty provisions.  No part of the Material
 * may be used, copied, reproduced, modified, published, uploaded,
 * posted, transmitted, distributed, or disclosed in any way without
 * Intel's prior express written permission.
 
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by Intel in writing.
 * 
 * $Workfile: $
 * $Revision: 1.1 $
 * $Author: pdamons $
 * $Date: 2005/11/28 20:55:11 $
 *
 */

#ifndef __DMRCONFIGURATION_H__
#define __DMRCONFIGURATION_H__
/**
    \file DMRConfiguration.h
    \ingroup DLNADMR
    \brief DLNA MediaRenderer Configuration

    <HR>
    INTEL CONFIDENTIAL<BR>
	Copyright©2002 - 2005 Intel Corporation.  All rights reserved.

	The source code contained or described herein and all documents
	related to the source code ("Material") are owned by Intel
	Corporation or its suppliers or licensors.  Title to the
	Material remains with Intel Corporation or its suppliers and
	licensors.  The Material contains trade secrets and proprietary
	and confidential information of Intel or its suppliers and
	licensors. The Material is protected by worldwide copyright and
	trade secret laws and treaty provisions.  No part of the Material
	may be used, copied, reproduced, modified, published, uploaded,
	posted, transmitted, distributed, or disclosed in any way without
	Intel's prior express written permission.

	No license under any patent, copyright, trade secret or other
	intellectual property right is granted to or conferred upon you
	by disclosure or delivery of the Materials, either expressly, by
	implication, inducement, estoppel or otherwise. Any license
	under such intellectual property rights must be express and
	approved by Intel in writing.

    \addtogroup DLNADMR DLNA MediaRenderer Microstack (DMR)
    \{
*/

/** \brief The values used to calculate how often the device advertises itself.

    This value should be between 120-1800 seconds!
*/
#define ADVERTISEMENT_PERIOD_SECONDS        1800

/** \brief Defines the maximum value of a volume level in the DMR implementation.

    This value cannot exceed 255.
*/
#define VOLUME_MAX                          100

/** \brief Defines the default value of a volume level in the DMR implementation.

    This value cannot exceed \ref VOLUME_MAX.
*/
#define VOLUME_FACTORYDEFAULT               15

/** \brief Defines the maximum value of a contrast level in the DMR implementation.

    This value cannot exceed 255.
*/
#define CONTRAST_MAX                        64

/** \brief Defines the default value of a contrast level in the DMR implementation.

    This value cannot exceed \ref CONTRAST_MAX.
*/
#define CONTRAST_FACTORYDEFAULT             32

/** \brief Defines the maximum value of a brightness level in the DMR implementation.

    This value cannot exceed 255.
*/
#define BRIGHTNESS_MAX                      64

/** \brief Defines the default value of a brightness level in the DMR implementation.

    This value cannot exceed \ref BRIGHTNESS_MAX.
*/
#define BRIGHTNESS_FACTORYDEFAULT           32

/* \} */

#endif /* __DMRCONFIGURATION_H__ */
