<?php if ( ! defined('BASEPATH')) exit('No direct script access allowed');

/*
isqrl helper
2013-10-05 - v 1.0 - isqrl_get_z
*/

if (!function_exists('isqrl_get_z'))
{
	function isqrl_get_z($x, $y, $url=NULL)
	{
		global $isqrl_error;

		$url or $url='https://isqrl.allogy.com/getz';

		$data['x']=$x;
		$data['y']=$y;

		#-------- ready to send the POST request ----------

		$ch = curl_init();
		#url_setopt($ch, CURLOPT_VERBOSE, 1);
		curl_setopt($ch, CURLOPT_URL, $url);
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
		curl_setopt($ch, CURLOPT_POSTFIELDS, $data);

		$response = curl_exec($ch);
		$info = curl_getinfo($ch);
		curl_close($ch);

		if ($response === false || $info['http_code']!=200)
		{
			$isqrl_error=trim($response);
			return null;
		}
		else
		{
			$isqrl_error=FALSE;
			return trim($response);
		}
	}
}
