import { http } from './http'
import type { FamilyVO, MemberVO } from '@/types/family'

export function createFamily(name: string): Promise<FamilyVO> {
  return http.post('/families', { name })
}

export function myFamily(): Promise<FamilyVO | null> {
  return http.get('/families/me')
}

export function getMembers(familyId: number): Promise<MemberVO[]> {
  return http.get(`/families/${familyId}/members`)
}

export function refreshInvite(familyId: number): Promise<string> {
  return http.post(`/families/${familyId}/invite`)
}

export function joinFamily(inviteCode: string): Promise<FamilyVO> {
  return http.post('/families/join', { inviteCode })
}
