import { http } from './http'
import type { LoginResponse, UserVO } from '@/types'

export function sendCode(phone: string): Promise<{ code: number }> {
  return http.post('/auth/sms-code', { phone })
}

export function loginByPhone(phone: string, code: string): Promise<LoginResponse> {
  return http.post('/auth/login/phone', { phone, code })
}

export function fetchMe(): Promise<UserVO> {
  return http.get('/auth/me')
}
