/*
 * INTEL CONFIDENTIAL
 * Copyright (c) 2002 - 2006 Intel Corporation.  All rights reserved.
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
 * $File: $
 * $Revision: 1.5 $
 * $Author: pdamons $
 * $Date: 2006/03/06 23:28:28 $
 *
 */

#ifndef __BITARRAY_H__
#define __BITARRAY_H__

/**
    \file BitArray.h
    \ingroup DLNADMR
	\brief Utility API for creating a bit array.

    \addtogroup DLNADMR DLNA MediaRenderer Microstack (DMR)

    \{
*/

/** \brief The type 'BitArray' is a 'void*'.
*/
typedef void *BitArray;


/** \brief Create a BitArray object.
	\param size The maximum size of the bit array in bits.
	\param initialValue The initial value of the bits in the array
	(must be 0 for an off state, and non-zero for a on state).

	\returns The created BitArray object or NULL if an error occurred.

	\warning The maximum size of the array is fixed but the usable size of the
			 array is initially set to zero.  This allows growth up to the maximum
			 size without a memory allocation and memory copy penilty.  Use the
			 \ref BitArray_ChangeSize method to change the current size.
*/
BitArray BitArray_Create(int size, int initialValue);

/** \brief Destroys all memory associated with the \ref BitArray object.
	\param array This is the 'this' pointer to the \ref BitArray instance.
*/
void BitArray_Destroy(BitArray array);

/** \brief Sets a specified bit to the specified value.
	\param array This is the 'this' pointer to the \ref BitArray instance.
	\param index The zero based index of the bit to set/reset.
	\param value Etiher 0 to reset the bit or non-zero to set the bit.

	\returns Zero (0) if the method fails or 1 if the method succeeds.
*/
int BitArray_SetBit(BitArray array, int index, int value);

/** \brief Gets the bit value at the specific index.
	\param array This is the 'this' pointer to the \ref BitArray instance.
	\param index The zero based index of the bit to get the state of.
	\returns 0 if the bit is reset, 1 if the bit is set or, -1 if the index
			 was out of range.
*/
int BitArray_GetBit(BitArray array, int index);

/** \brief Tests to see if all the bits in the useable array have the given
		   value.
	\param array This is the 'this' pointer to the \ref BitArray instance.
	\param bit The value to compare to.  Use 0 to test for the bits being reset
		   (0) or non-zero to test for the bits beign set (1).
	\returns Zero (0) if the bits are not all equal to the test value or (1) if
			 all the bits are equal to the test value.
*/
int BitArray_TestAllBitsEqualTo(BitArray array, int bit);

/** \brief Sets all bits in the useable array size to the given value.
	\param array This is the 'this' pointer to the \ref BitArray instance.
	\param bit The value to set all of the bits in the array to.
*/
void BitArray_Reset(BitArray array, int bit);

/** \brief Change the size of the usable array to the given size.
	\param array This is the 'this' pointer to the \ref BitArray instance.
	\param size The new size of the array.  Must be 0-MaxSize inclusive.
*/
void BitArray_ChangeSize(BitArray array, int size);

/* \} */
#endif /* __BITARRAY_H__ */
